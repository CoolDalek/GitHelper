package model

import doobie.util._
import monix.newtypes.NewtypeWrapped
import pureconfig.ConfigReader

object ApiToken extends NewtypeWrapped[String] {

  implicit val read: ConfigReader[ApiToken] = derive[ConfigReader]

  implicit val get: Get[ApiToken] = derive[Get]

  implicit val put: Put[ApiToken] = derive[Put]

}