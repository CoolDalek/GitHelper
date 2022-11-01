package dao.impl

import cats.data.OptionT
import cats.effect.MonadCancelThrow
import cats.syntax.all.*
import doobie.*
import doobie.implicits.{*, given}

trait DoobieDao[F[_]]:

  def xa: Transactor[F]

  def update(fragment: => Fragment)
            (using MonadCancelThrow[F]): F[Unit] =
    fragment.update.run
      .transact(xa).void

  def selectOne[T: Read](fragment: => Fragment)
                        (using MonadCancelThrow[F]): OptionT[F, T] =
    OptionT(
      fragment.query[T].option.transact(xa)
    )

end DoobieDao
