package integration

import model.{ApiToken, Profile}

trait GithubClient[F[_]] {

  def profile(token: ApiToken): F[Profile]

}