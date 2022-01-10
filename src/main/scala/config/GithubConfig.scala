package config

import model.ApiToken
import pureconfig.ConfigReader
import pureconfig.generic.semiauto._

case class GithubConfig(apiToken: ApiToken)
object GithubConfig {

  implicit val reader: ConfigReader[GithubConfig] = deriveReader[GithubConfig]

}