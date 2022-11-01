package config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

case class CryptoConfig(
                         password: String,
                       ) derives ConfigReader
