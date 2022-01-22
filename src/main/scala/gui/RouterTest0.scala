package gui

import cats.effect.Spawn
import cats.syntax.all._
import cats.{Applicative, Parallel}
import swing.Screen.ScreenCompanion
import swing.{Router, StatelessScreen, Swing}

import javax.swing.{JButton, JPanel}

class RouterTest0[F[_]: Swing: Spawn: Parallel](router: Router[F])
  extends StatelessScreen[F](router, RouterTest0.route) {

  override type Model = Unit

  override def makeModel: F[Model] = Applicative[F].unit

  override protected def view(model: Model): View =
    new JPanel() {
      add(
        new JButton("Go to router test 1") { button =>
          moveTo(RouterTest1.route, button)
        }
      )
    }

}
object RouterTest0 extends ScreenCompanion {

  def make[F[_]: Swing: Spawn: Parallel](router: Router[F]): F[Unit] =
    new RouterTest0(router).build.void

}