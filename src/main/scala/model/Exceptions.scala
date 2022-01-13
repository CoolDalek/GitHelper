package model

object Exceptions {

  case class GithubException(message: String) extends RuntimeException(message)

}