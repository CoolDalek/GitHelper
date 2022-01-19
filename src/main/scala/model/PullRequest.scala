package model

import serialization._
import sttp.model.Uri

case class PullRequest(
                        htmlUrl: Uri,
                      )
object PullRequest {

  implicit val read: Reader[PullRequest] = macroR[PullRequest]

}