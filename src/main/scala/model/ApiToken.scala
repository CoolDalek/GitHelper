package model

import cats.Show
import doobie.util._
import effects.{PrepareDecryption, PrepareEncryption}
import monix.newtypes.NewtypeWrapped
import pureconfig.ConfigReader

object ApiToken extends NewtypeWrapped[String] {

  implicit val read: ConfigReader[ApiToken] = derive[ConfigReader]

  implicit val show: Show[ApiToken] = derive[Show]

  implicit val get: Get[ApiToken] = derive[Get]

  implicit val put: Put[ApiToken] = derive[Put]

  implicit val prepareEncryption: PrepareEncryption[ApiToken] =
    PrepareEncryption.asString(_.value)

  implicit val prepareDecryption: PrepareDecryption[ApiToken] =
    PrepareDecryption.fromString(apply)

}