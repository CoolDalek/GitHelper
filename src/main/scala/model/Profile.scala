package model

import serialization._

case class Profile(
                    login: String,
                  )

object Profile {

  implicit val read: Reader[Profile] = macroR[Profile]

}