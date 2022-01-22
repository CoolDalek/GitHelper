package gui

import cats.Parallel
import cats.effect.Async
import cats.syntax.all._
import model.Notification
import service.NotificationService
import sttp.client3.UriContext
import swing.Router.Route
import swing.Screen.ScreenCompanion
import swing._

import javax.swing.{JScrollPane, JTable}

class NotificationScreen[F[+_]: Swing: Async: Parallel](
                                                         notificationService: NotificationService[F],
                                                         router: Router[F],
                                                         route: Route,
                                                       ) extends StatefulScreen[F](router, route) {
  override type State = Table[Notification]
  override type Model = SwingTableModel[F, Notification]

  override def loadState: F[State] = Table.one[F, Notification](
    Notification("Fetching", uri"https://github.com/", Seq.empty),
    "PR's",
  )

  override def saveState: F[Unit] = ???

  override def makeModel(state: State): F[Model] = {
    val dataSource = notificationService.notifications.evalMap { seq =>
      Table.column[F, Notification](seq.toArray)
    }
    SwingTableModel.make[F, Notification](dataSource, state)
  }

  override protected def view(model: Model): View =
    new JScrollPane(
      new JTable(model) {
        setFillsViewportHeight(true)
      }
    )

}
object NotificationScreen extends ScreenCompanion {

  def make[F[+_]: Swing: Async: Parallel](notificationService: NotificationService[F],
                                router: Router[F]): F[Unit] =
    new NotificationScreen(notificationService, router, route).build.void

}