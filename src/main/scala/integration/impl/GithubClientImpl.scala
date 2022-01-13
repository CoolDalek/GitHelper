package integration.impl

import serialization._
import cats.MonadThrow
import cats.syntax.all._
import integration.GithubClient
import model.Exceptions.GithubException
import model.{ApiToken, Profile}
import sttp.client3._
import sttp.model.HeaderNames

class GithubClientImpl[F[_]: MonadThrow](
                                           httpClient: SttpBackend[F, Any],
                                         ) extends GithubClient[F] {

  override def profile(token: ApiToken): F[Profile] =
    for {
      response <- basicRequest
        .header(HeaderNames.Authorization, s"token ${ApiToken.value(token)}")
        .get(uri"https://api.github.com/user")
        .send(httpClient)
      body <- response.body
        .leftMap(GithubException)
        .liftTo[F]
    } yield read[Profile](body)

}
object GithubClientImpl {

  def make[F[+_]: MonadThrow](httpClient: SttpBackend[F, Any]): F[GithubClient[F]] =
    new GithubClientImpl(httpClient).pure[F]
}