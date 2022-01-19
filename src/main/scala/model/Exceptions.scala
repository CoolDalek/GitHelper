package model

import sttp.model.Uri

object Exceptions {

  case class GithubException(uri: Uri, message: String) extends RuntimeException(s"Response from $uri: $message")

  case object NoTokenProvided extends RuntimeException("No authorization token provided.")

}