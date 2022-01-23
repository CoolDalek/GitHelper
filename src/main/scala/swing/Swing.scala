package swing

import cats.Defer
import cats.effect.Async
import effects.SummonerK

import javax.swing.SwingUtilities
import scala.util.control.NonFatal

trait Swing[F[_]] extends Defer[F] {

  type Callback[T] = Throwable Either T => Unit

  def onUI[T](thunk: => T): F[T]

  def fromUI[T](task: Callback[T] => Unit): F[T]

  def suspend[T](thunk: => T): F[T]

}
object Swing extends SummonerK[Swing] {

  private def invokeLater[T](thunk: => T): Unit =
    SwingUtilities.invokeLater(() => thunk)

  implicit def asyncSwing[F[_]: Async](implicit derive: Defer[F]): Swing[F] = new Swing[F] {

    override def onUI[T](thunk: => T): F[T] =
      Async[F].async_[T] { callback =>
        invokeLater {
          val result = try {
            Right(thunk)
          } catch {
            case NonFatal(exc) =>
              println(exc)
              Left(exc)
          }
          callback(result)
        }
      }

    override def fromUI[T](task: Callback[T] => Unit): F[T] =
      Async[F].async_[T] { callback =>
        invokeLater {
          try {
            task(callback)
          } catch {
            case NonFatal(exc) =>
              println(exc)
              callback(Left(exc))
          }
        }
      }

    override def defer[A](fa: => F[A]): F[A] = derive.defer(fa)

    /*
    * As alternative: Defer[F].defer(Functor[F].pure(thunk))
    * Or: Applicative[F].unit.map(_ => thunk)
    * Or: Functor[F].lift(_ => thunk)
    * */
    override def suspend[T](thunk: => T): F[T] =
      Async[F].delay(thunk)

  }

}