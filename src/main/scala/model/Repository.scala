package model

import serialization._
import sttp.model.Uri

case class Repository(
                       name: String,
                       htmlUrl: Uri,
                       pullsUrl: Uri,
                     ) {

  val pullsBaseUrl: Uri = {
    val url = pullsUrl.toString()
    val base = url.takeWhile(_ != '%')
    Uri.unsafeParse(base)
  }

}
object Repository {

  implicit val read: Reader[Repository] = macroR[Repository]

}