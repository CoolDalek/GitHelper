package integration.impl

import cats.MonadThrow
import cats.syntax.all.*
import integration.GithubClient
import model.*
import serialization.{*, given}
import sttp.client3.*
import sttp.model.{HeaderNames, Uri}

class GithubClientImpl[F[_]: MonadThrow](
                                           httpClient: SttpBackend[F, Any],
                                         ) extends GithubClient[F] {
  
  private def get[T: Codec](token: ApiToken, uri: Uri): F[T] =
    for {
      response <- basicRequest
        .header(HeaderNames.Authorization, s"token $token")
        .get(uri)
        .send(httpClient)
      body <- response.body
        .leftMap(x => GithubException(uri, x))
        .liftTo[F]
    } yield read[T](body)

  override def profile(token: ApiToken): F[Profile] =
    get[Profile](token, uri"https://api.github.com/user")

  override def repositories(token: ApiToken): F[Seq[Repository]] =
    get[Seq[Repository]](token, uri"https://api.github.com/user/repos")

  override def pullRequests(repository: Repository, token: ApiToken): F[Seq[PullRequest]] =
    get[Seq[PullRequest]](token, repository.pullsBaseUrl)

}
object GithubClientImpl {

  def make[F[+_]: MonadThrow](httpClient: SttpBackend[F, Any]): F[GithubClient[F]] =
    new GithubClientImpl(httpClient).pure[F]
}