import cats.effect.{ExitCode, IO, IOApp}
import config.{CryptoConfig, DbConfig}
import dao.impl.TokenDaoImpl
import doobie.util.transactor.Transactor
import gui.Gui
import integration.impl.GithubClientImpl
import model.ApiToken
import provider.impl.CryptoProviderImpl
import pureconfig.{ConfigObjectSource, ConfigReader, ConfigSource}
import service.impl.MigrationServiceImpl
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

import scala.reflect.ClassTag

object Main extends IOApp {

  private val configSource: ConfigObjectSource = ConfigSource.default

  private def loadConfig[T: ConfigReader: ClassTag](path: String): IO[T] = IO {
    configSource.at(path).loadOrThrow[T]
  }

  override def run(args: List[String]): IO[ExitCode] =
    for {
      gui <- Gui.make[IO]
      join <- gui.start
      dbConfig <- loadConfig[DbConfig]("db")
      xa = Transactor.fromDriverManager[IO](
        driver = dbConfig.driver,
        url = dbConfig.url,
        user = dbConfig.user,
        pass = dbConfig.password,
      )
      cryptoConfig <- loadConfig[CryptoConfig]("crypto")
      migrations <- MigrationServiceImpl.make[IO](xa)
      _ <- if(dbConfig.dropOnStartup) migrations.dropDb else IO.unit
      _ <- migrations.needMigration.ifM(migrations.migrate, IO.unit)
      cryptoProvider <- CryptoProviderImpl.make[IO](cryptoConfig)
      tokensDao <- TokenDaoImpl.make[IO](xa, cryptoProvider)
      httpClient <- AsyncHttpClientCatsBackend[IO]()
      github <- GithubClientImpl.make[IO](httpClient)
      _ <- join
      _ <- httpClient.close()
    } yield ExitCode.Success

}