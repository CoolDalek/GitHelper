package dao.impl

import cats.Monad
import cats.effect.MonadCancelThrow
import cats.syntax.all._
import doobie.Fragment
import model.ApiToken
import provider.CryptoProvider

trait EncryptedDao[F[_]] extends DoobieDao[F] {

  def cryptoProvider: CryptoProvider[F]

  def withEncryption[T](token: ApiToken)
                       (body: String => F[T])
                       (implicit ev: Monad[F]): F[T] =
    cryptoProvider.encrypt(token.value).flatMap(body)

  def updateWithEncryption(token: ApiToken)
                          (fragment: String => Fragment)
                          (implicit ev: MonadCancelThrow[F]): F[Unit] =
    withEncryption(token)(x => update(fragment(x)))

}