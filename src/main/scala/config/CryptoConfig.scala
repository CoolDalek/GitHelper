package config

import pureconfig.ConfigReader
import pureconfig.generic.semiauto._

case class CryptoConfig(
                         password: String,
                       )
object CryptoConfig {

  implicit val reader: ConfigReader[CryptoConfig] = deriveReader[CryptoConfig]

}