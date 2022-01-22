package swing

import cats.{Applicative, Parallel}
import cats.effect.Spawn
import cats.syntax.all._
import swing.Router.Route

abstract class StatefulScreen[F[_]: Swing: Spawn: Parallel](
                                                             router: Router[F],
                                                             route: Route,
                                                           ) extends Screen[F](router, route) {
  type State

  def loadState: F[State]

  def saveState: F[Unit]

  override final def moveTo[T: ActionListener](to: Route, on: T): Unit =
    actionListener(on) {
      if(to != route) {
        (saveState, cleanup >> router.move(route, to))
          .parMapN { case(_, _) => () }
      } else Applicative[F].unit
    }

  override final def makeModel: F[Model] =
    loadState.flatMap(makeModel)

  def makeModel(state: State): F[Model]

}