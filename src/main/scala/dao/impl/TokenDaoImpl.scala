package dao.impl

import cats.effect.MonadCancelThrow
import cats.syntax.all._
import dao.TokenDao
import model.ApiToken
import doobie._
import doobie.implicits._
import provider.CryptoProvider

class TokenDaoImpl[F[_]: MonadCancelThrow](
                                            val xa: Transactor[F],
                                            val cryptoProvider: CryptoProvider[F],
                                          ) extends TokenDao[F] with EncryptedDao[F] {

  override def getToken: F[Option[ApiToken]] =
    for {
      maybeToken <- selectOne[ApiToken] {
        sql"select * from api_tokens"
      }
      maybeToken
    }

  override def addToken(apiToken: ApiToken): F[Unit] =
    updateWithEncryption(apiToken) { encrypted =>
      sql"insert into api_tokens values($encrypted)"
    }

  override def removeToken(apiToken: ApiToken): F[Unit] =
    updateWithEncryption(apiToken) { encrypted =>
      sql"delete from api_tokens where body = $encrypted"
    }

}
object TokenDaoImpl {

  def make[F[+_]: MonadCancelThrow](xa: Transactor[F], cryptoProvider: CryptoProvider[F]): F[TokenDao[F]] =
    new TokenDaoImpl[F](xa, cryptoProvider).pure[F]

}