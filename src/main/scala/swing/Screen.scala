package swing

import cats.Applicative
import cats.effect.Spawn
import cats.syntax.all._
import swing.Router.Route
import swing.Screen.ScreenView

import javax.swing.JComponent

abstract class Screen[F[_]: Swing: Spawn](
                                           router: Router[F],
                                           val route: Route,
                                         ) extends UIComponent[F]{
  final type View = ScreenView
  final type Built = F[View]

  override final def makeView(model: Model): F[View] = super.makeView(model)

  protected def moveTo[T: ActionListener](to: Route, on: T): Unit =
    actionListener(on) {
      if(to != route) {
        cleanup >> router.move(route, to)
      } else Applicative[F].unit
    }

  override final def build: F[Built] = for {
    maybeBuilt <- router.requestBuild(route)
    view <- maybeBuilt match {
      case Some(view) =>
        view.pure[F]
      case None =>
        val viewF = for {
          _ <- prepare
          model <- makeModel
          view <- makeView(model)
          _ <- handleEvents
        } yield view
        router.commitBuild(route, viewF) as viewF
    }
  } yield view

}
object Screen {

  type ScreenView = JComponent

  trait ScreenCompanion {

    val route: Route = new Route

  }

}