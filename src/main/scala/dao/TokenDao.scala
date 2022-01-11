package dao

import model.ApiToken

trait TokenDao[F[_]] {

  def getToken: F[Option[ApiToken]]

  def addToken(apiToken: ApiToken): F[Unit]

  def removeToken(apiToken: ApiToken): F[Unit]

}