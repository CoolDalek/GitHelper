package dao.impl

import cats.data.OptionT
import cats.effect.*
import cats.syntax.all.*
import dao.TokenDao
import doobie.*
import doobie.implicits.{*, given}
import model.ApiToken
import provider.CryptoProvider

class TokenDaoImpl[F[_]: MonadCancelThrow](
                                            val xa: Transactor[F],
                                            val cryptoProvider: CryptoProvider[F],
                                          ) extends TokenDao[F] with DoobieDao[F]:

  def encryptedUpdate(token: ApiToken)(sql: String => Fragment): F[Unit] =
    for {
      encrypted <- cryptoProvider.encryptString(token)
      _ <- update(sql(encrypted))
    } yield ()

  def decryptedSelect(sql: => Fragment): OptionT[F, ApiToken] =
    for {
      encrypted <- selectOne[String](sql)
      token <- OptionT.liftF(
        cryptoProvider.decryptString[ApiToken](encrypted)
      )
    } yield token

  def decrypt(text: String): F[ApiToken] =
    cryptoProvider.decryptString[ApiToken](text)

  override def getToken: OptionT[F, ApiToken] =
    decryptedSelect {
      sql"select * from api_tokens"
    }

  override def addToken(apiToken: ApiToken): F[Unit] =
    encryptedUpdate(apiToken) { encrypted =>
      sql"insert into api_tokens values($encrypted)"
    }

  override def removeToken(apiToken: ApiToken): F[Unit] =
    encryptedUpdate(apiToken) { encrypted =>
      sql"delete from api_tokens where body = $encrypted"
    }

object TokenDaoImpl:

  def make[F[+_]: MonadCancelThrow](xa: Transactor[F], cryptoProvider: CryptoProvider[F]): F[TokenDao[F]] =
    new TokenDaoImpl[F](xa, cryptoProvider).pure[F]

end TokenDaoImpl
