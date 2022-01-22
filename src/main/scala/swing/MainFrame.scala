package swing

import cats.{Applicative, Defer}
import cats.effect.kernel.Concurrent
import cats.syntax.all._
import swing.Router.Route

import java.awt.event.{WindowAdapter, WindowEvent}
import javax.swing.JFrame

abstract class MainFrame[F[_]: Swing: Concurrent](
                                                   initialScreen: Route,
                                                 ) extends UIComponent[F] {
  final type Model = Unit
  final type View = JFrame
  final type Built = (Router[F], F[Unit])

  private def registerClosing(frame: JFrame): F[Unit] =
    Swing[F].fromUI[Unit] { callback =>
      frame.addWindowListener(
        new WindowAdapter {
          override def windowClosing(e: WindowEvent): Unit =
            callback(RightUnit)
        }
      )
    }

  override final def makeModel: F[Unit] = Applicative[F].unit

  override final def makeView(model: Unit): F[JFrame] = Swing[F].onUI {
    val frame = view(model)
    frame.setVisible(true)
    frame
  }

  override final def build: F[Built] = {
    for {
      model <- makeModel
      view <- makeView(model)
      router = Router[F](view)
      _ <- router.initialScreen(initialScreen)
    } yield router -> registerClosing(view)
  }

}