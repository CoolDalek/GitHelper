package config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

import scala.concurrent.duration.FiniteDuration

case class NotificationConfig(
                               pollingDelay: FiniteDuration,
                             ) derives ConfigReader
