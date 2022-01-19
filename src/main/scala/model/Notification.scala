package model

import cats.Show
import sttp.model.Uri

case class Notification(
                         repoName: String,
                         repoUrl: Uri,
                         pullUrls: Seq[Uri],
                       )
object Notification {

  implicit val show: Show[Notification] =
    (notification: Notification) => s"${notification.repoName}(${notification.pullUrls.length})"

}