package dao

import cats.data.OptionT
import model.ApiToken

trait TokenDao[F[_]] {

  def getToken: OptionT[F, ApiToken]

  def addToken(apiToken: ApiToken): F[Unit]

  def removeToken(apiToken: ApiToken): F[Unit]

}