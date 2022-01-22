import cats.effect.std.Dispatcher
import cats.effect.{ExitCode, IO, IOApp}
import config.{CryptoConfig, DbConfig, NotificationConfig}
import dao.impl.TokenDaoImpl
import doobie.util.transactor.Transactor
import gui.{MainUI, NotificationScreen, RouterTest0, RouterTest1}
import integration.impl._
import provider.impl._
import pureconfig.{ConfigObjectSource, ConfigReader, ConfigSource}
import service.impl._
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import swing.Router

import scala.reflect.ClassTag

object Main extends IOApp {

  private val configSource: ConfigObjectSource = ConfigSource.default

  private def loadConfig[T: ConfigReader: ClassTag](path: String): IO[T] = IO {
    configSource.at(path).loadOrThrow[T]
  }

  override def run(args: List[String]): IO[ExitCode] =
    for {
      makeUI <- MainUI.make[IO]
      (router, join) = makeUI
      _ <- RouterTest0.make(router)
      _ <- RouterTest1.make(router)
      /*
      dbConfig <- loadConfig[DbConfig]("db")
      xa = Transactor.fromDriverManager[IO](
        driver = dbConfig.driver,
        url = dbConfig.url,
        user = dbConfig.user,
        pass = dbConfig.password,
      )
      cryptoConfig <- loadConfig[CryptoConfig]("crypto")
      notificationConfig <- loadConfig[NotificationConfig]("notification")
      migrations <- MigrationServiceImpl.make[IO](xa)
      _ <- if(dbConfig.dropOnStartup) migrations.dropDb else IO.unit
      _ <- migrations.needMigration.ifM(migrations.migrate, IO.unit)
      cryptoProvider <- CryptoProviderImpl.make[IO](cryptoConfig)
      tokensDao <- TokenDaoImpl.make[IO](xa, cryptoProvider)
      httpClient <- Dispatcher[IO].use { dispatcher =>
        HttpClientFs2Backend[IO](dispatcher)
      }
      github <- GithubClientImpl.make[IO](httpClient)
      notifications <- NotificationServiceImpl.make[IO](
        notificationConfig,
        github,
        tokensDao,
      )
      _ <- NotificationScreen.make(notifications, router)
      */
      _ <- join
      //_ <- httpClient.close()
    } yield ExitCode.Success

}