package dao.impl

import cats.effect.MonadCancelThrow
import cats.syntax.all._
import dao.TokenDao
import model.ApiToken
import doobie._
import doobie.implicits._

class TokenDaoImpl[F[_]: MonadCancelThrow](xa: Transactor[F]) extends TokenDao[F] {

  override def getToken: F[Option[ApiToken]] =
    sql"select * from api_tokens"
      .query[ApiToken]
      .option
      .transact(xa)

  override def addToken(apiToken: ApiToken): F[Unit] =
    sql"insert into api_tokens values $apiToken"
      .update
      .run
      .transact(xa)
      .void

  override def removeToken(apiToken: ApiToken): F[Unit] =
    sql"delete from api_tokens where body = $apiToken"
      .update
      .run
      .transact(xa)
      .void

}
object TokenDaoImpl {

  def make[F[+_]: MonadCancelThrow](xa: Transactor[F]): F[TokenDao[F]] =
    new TokenDaoImpl[F](xa).pure[F]

}