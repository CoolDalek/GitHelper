import cats.effect.{ExitCode, IO, IOApp}
import config.GithubConfig
import gui.Gui
import integration.impl.GithubClientImpl
import pureconfig.{ConfigObjectSource, ConfigReader, ConfigSource}

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
      gitConfig <- loadConfig[GithubConfig]("github")
      gitClient <- GithubClientImpl.make[IO](gitConfig)
      _ <- join
    } yield ExitCode.Success

}