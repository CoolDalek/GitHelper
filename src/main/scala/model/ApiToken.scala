package model

import monix.newtypes.NewtypeWrapped
import pureconfig.ConfigReader

object ApiToken extends NewtypeWrapped[String] {

  implicit val read: ConfigReader[ApiToken] = derive[ConfigReader]

}