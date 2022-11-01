package swing

import cats.Defer
import cats.effect.Async
import effects.SummonerK

import javax.swing.SwingUtilities
import scala.util.control.NonFatal

trait Swing[F[_]] extends Defer[F]:

  type Callback[T] = Either[Throwable, T] => Unit

  def onUI[T](thunk: => T): F[T]

  def fromUI[T](task: Callback[T] => Unit): F[T]

object Swing extends SummonerK[Swing]:

  private inline def invokeLater[T](inline thunk: => T): Unit =
    SwingUtilities.invokeLater(() => thunk)

  given [F[_]: Async]: Swing[F] with


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
    end onUI

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
    end fromUI

    override def defer[A](fa: => F[A]): F[A] = Async[F].defer(fa)
    
  end given

end Swing
