package gui

import cats.effect.Concurrent
import swing.{MainFrame, Router, Swing}

import java.awt.Dimension
import javax.swing.{JFrame, JLabel, JPanel, WindowConstants}

class MainUI[F[_]: Swing: Concurrent] extends MainFrame[F](RouterTest0.route) {

  override def view(model: Model): View =
    new JFrame("Github helper") {
      setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
      setMinimumSize(new Dimension(300, 400))
      setResizable(false)
      setContentPane(
        new JPanel() {
          add(new JLabel("Welcome to GitHelper app."))
        }
      )
    }

}
object MainUI {

  def make[F[_]: Swing: Concurrent]: F[(Router[F], F[Unit])] =
    new MainUI[F].build

}