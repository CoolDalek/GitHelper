package integration.impl

import cats.effect.Sync
import cats.syntax.all._
import config.GithubConfig
import integration.GithubClient
import org.kohsuke.github._

import scala.util.control.NonFatal

class GithubClientImpl[F[_]: Sync](git: GitHub) extends GithubClient[F] {

  val self: F[GHMyself] = Sync[F].blocking(git.getMyself)

  val login: F[String] = for {
    me <- self
  } yield me.getLogin

}
object GithubClientImpl {

  def make[F[+_]: Sync](config: GithubConfig): F[GithubClient[F]] =
    try {
      val git = new GitHubBuilder()
        .withAppInstallationToken(config.apiToken.value)
        .build()
      new GithubClientImpl[F](git).pure[F]
    } catch {
      case NonFatal(exc) =>
        exc.raiseError[F, GithubClient[F]]
    }

}