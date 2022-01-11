import cats.effect.{ExitCode, IO, IOApp}
import config.DbConfig
import doobie.util.transactor.Transactor
import gui.Gui
import integration.impl.GithubClientImpl
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
      migrations <- MigrationServiceImpl.make[IO](xa)
      _ <- if(dbConfig.dropOnStartup) migrations.dropDb else IO.unit
      _ <- migrations.needMigration.ifM(migrations.migrate, IO.unit)
      httpClient <- AsyncHttpClientCatsBackend[IO]()
      _ <- GithubClientImpl.make[IO](httpClient)
      _ <- join
      _ <- httpClient.close()
    } yield ExitCode.Success

}