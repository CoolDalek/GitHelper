package gui

import cats.effect.Async
import cats.syntax.all._
import effects.Swing
import gui.Gui.RightUnit

import java.awt.event.{WindowAdapter, WindowEvent}
import javax.swing.{JFrame, WindowConstants}

class Gui[F[_]: Async: Swing] {

  val mainFrame: JFrame = new JFrame("Github helper") {
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
    setSize(300, 400)
    setResizable(false)
  }

  def registerClosing: F[Unit] =
    Async[F].async_[Unit] { callback =>
      mainFrame.addWindowListener(
        new WindowAdapter {
          override def windowClosing(e: WindowEvent): Unit = {
            callback(RightUnit)
          }
        }
      )
    }

  def start: F[F[Unit]] = Swing[F].onUI {
    mainFrame.setVisible(true)
    registerClosing
  }

}
object Gui {

  def make[F[_]: Async: Swing]: F[Gui[F]] = new Gui[F].pure[F]

  final val RightUnit: Either[Throwable, Unit] = Right(())

}