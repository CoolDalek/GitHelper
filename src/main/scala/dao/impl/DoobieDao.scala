package dao.impl

import cats.data.OptionT
import cats.effect.MonadCancelThrow
import cats.syntax.all._
import doobie._
import doobie.implicits._

trait DoobieDao[F[_]] {

  def xa: Transactor[F]

  def update(fragment: => Fragment)
            (implicit ev: MonadCancelThrow[F]): F[Unit] =
    fragment.update.run
      .transact(xa).void

  def selectOne[T: Read](fragment: => Fragment)
                        (implicit ev: MonadCancelThrow[F]): OptionT[F, T] =
    OptionT(
      fragment.query[T].option.transact(xa)
    )

}
