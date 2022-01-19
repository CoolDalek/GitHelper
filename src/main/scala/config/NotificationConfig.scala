package config

import pureconfig.ConfigReader
import pureconfig.generic.semiauto._

import scala.concurrent.duration.FiniteDuration

case class NotificationConfig(
                               pollingDelay: FiniteDuration,
                             )
object NotificationConfig {

  implicit val reader: ConfigReader[NotificationConfig] = deriveReader[NotificationConfig]

}