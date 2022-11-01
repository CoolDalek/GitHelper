import cats.effect.std.Dispatcher
import cats.effect.*
import config.{CryptoConfig, DbConfig, NotificationConfig}
import dao.impl.TokenDaoImpl
import doobie.util.transactor.Transactor
import gui.*
import integration.impl.*
import provider.impl.*
import pureconfig.error.ConfigReaderException
import pureconfig.{ConfigObjectSource, ConfigReader, ConfigSource}
import service.impl.*
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import swing.{Delayed, DependencyGraph, Router, Dependency}

import scala.reflect.ClassTag

object Main extends IOApp:

  private val configSource: ConfigObjectSource = ConfigSource.default

  private def loadConfig[T: ConfigReader: ClassTag](path: String): IO[T] =
    IO.fromEither {
      configSource.at(path)
        .load[T]
        .left
        .map(x => new ConfigReaderException[T](x))
    }
  end loadConfig

  override def run(args: List[String]): IO[ExitCode] =
    for {
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
      resource = for {
        httpClient <- HttpClientFs2Backend.resource[IO]()
        github <- Resource.eval(GithubClientImpl.make[IO](httpClient))
        notifications <- Resource.eval {
          NotificationServiceImpl.make[IO](
            notificationConfig,
            github,
            tokensDao,
          )
        }
        router <- Router.make[IO]
        graph = DependencyGraph.make[IO]
        given DependencyGraph[IO] = graph
        dependencies = graph.require[NotificationScreen[IO]]
          .edge { screen =>
            val mainUI = MainUI[IO](router, screen)
            graph.provide(IO.pure("""???"""))
              .map(_ => mainUI)
          }
        window <- dependencies.resolve
        windowClosing = router.moveToWindow(
          Delayed.const[IO, MainUI[IO]](window)
        )
      } yield windowClosing
      _ <- resource.use(identity)
    } yield ExitCode.Success
  end run

end Main
