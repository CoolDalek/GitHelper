package swing

import cats.Parallel
import cats.effect.kernel.Spawn
import swing.Router.Route

abstract class StatelessScreen[F[_]: Swing: Spawn: Parallel](
                                                    router: Router[F],
                                                    route: Route,
                                                  ) extends Screen[F](router, route) {

  override final def moveTo[T: ActionListener](to: Route, on: T): Unit = super.moveTo(to, on)

}
