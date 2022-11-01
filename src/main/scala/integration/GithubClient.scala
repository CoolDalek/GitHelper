package integration

import model.*

trait GithubClient[F[_]] {

  def profile(token: ApiToken): F[Profile]

  def repositories(token: ApiToken): F[Seq[Repository]]

  def pullRequests(repository: Repository, token: ApiToken): F[Seq[PullRequest]]

}