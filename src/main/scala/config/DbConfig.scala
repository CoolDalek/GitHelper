package config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

case class DbConfig(
                     driver: String,
                     url: String,
                     user: String,
                     password: String,
                     dropOnStartup: Boolean,
                   ) derives ConfigReader
