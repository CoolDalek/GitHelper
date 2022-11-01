package model

import serialization.{*, given}
import sttp.model.Uri

case class PullRequest(
                        htmlUrl: Uri,
                      )
object PullRequest {

  given Codec[PullRequest] = deriveCodec[PullRequest]

}