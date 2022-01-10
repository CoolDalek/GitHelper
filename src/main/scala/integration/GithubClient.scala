package integration

trait GithubClient[F[_]] {

  def login: F[String]

}