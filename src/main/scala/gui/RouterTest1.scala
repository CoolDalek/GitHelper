package gui
/*
import cats.effect.Spawn
import cats.syntax.all.*
import cats.{Applicative, Parallel}
import swing.Screen.ScreenCompanion
import swing.{Router, StatelessScreen, Swing}

import javax.swing.{JButton, JPanel}

class RouterTest1[F[_]: Swing: Spawn: Parallel](router: Router[F])
  extends StatelessScreen[F](router, RouterTest1.route) {

  override type Model = Unit

  override def makeModel: F[Model] = Applicative[F].unit

  override protected def view(model: Model): View =
    new JPanel() {
      add(
        new JButton("Go to router test 0") { button =>
          moveTo(RouterTest0.route, button)
        }
      )
    }

}
object RouterTest1 extends ScreenCompanion {

  def make[F[_]: Swing: Spawn: Parallel](router: Router[F]): F[Unit] =
    new RouterTest1(router).build.void

}
*/