package integration.impl

import cats.Applicative
import cats.syntax.all._
import integration.GithubClient
import sttp.client3.SttpBackend

class GithubClientImpl[F[_]](httpClient: SttpBackend[F, Any]) extends GithubClient[F] {

  override def login: F[String] = ???

}
object GithubClientImpl {

  def make[F[+_]: Applicative](httpClient: SttpBackend[F, Any]): F[GithubClient[F]] =
    new GithubClientImpl(httpClient).pure[F]
}