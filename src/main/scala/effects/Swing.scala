package effects

import cats.effect.Async

import javax.swing.SwingUtilities
import scala.util.control.NonFatal

trait Swing[F[_]] {

  def onUI[T](thunk: => T): F[T]

}
object Swing extends SummonerK[Swing] {

  private def invokeLater[T](thunk: => T): Unit =
    SwingUtilities.invokeLater(() => thunk)

  implicit def asyncSwing[F[_]: Async]: Swing[F] = new Swing[F] {

    override def onUI[T](thunk: => T): F[T] = {
      Async[F].async_[T] { callback =>
        invokeLater {
          val result = try {
            Right(thunk)
          } catch {
            case NonFatal(exc) =>
              Left(exc)
          }
          callback(result)
        }
      }
    }
  }

}