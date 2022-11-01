package model

import serialization.*

case class Profile(
                    login: String,
                  )
object Profile:
  given Codec[Profile] = deriveCodec[Profile]
