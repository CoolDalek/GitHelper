package gui

import cats.effect.Async
import cats.syntax.all._
import effects.Swing
import gui.Gui.RightUnit
import model.{Notification, SwingTableModel, Table}
import service.NotificationService
import sttp.client3.UriContext

import java.awt.Dimension
import java.awt.event.{WindowAdapter, WindowEvent}
import javax.swing.{JFrame, JScrollPane, JTable, WindowConstants}

class Gui[F[+_]: Async: Swing](notificationService: NotificationService[F]) {

  def makeView(model: SwingTableModel[F, Notification]): JFrame =
    new JFrame("Github helper") {
      setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
      setMinimumSize(new Dimension(300, 400))
      setResizable(false)
      setContentPane(
        new JScrollPane(
          new JTable(model) {
            setFillsViewportHeight(true)
          }
        )
      )
    }

  def makeModel: F[SwingTableModel[F, Notification]] = {
    val dataSource = notificationService.notifications.evalMap { seq =>
      Table.column[F, Notification](seq.toArray)
    }
    for {
      init <- Table.one[F, Notification](
        Notification("Fetching", uri"https://github.com/", Seq.empty),
        "PR's",
      )
      model <- SwingTableModel.make[F, Notification](dataSource, init)
    } yield model
  }

  def registerClosing(frame: JFrame): F[Unit] =
    Async[F].async_[Unit] { callback =>
      frame.addWindowListener(
        new WindowAdapter {
          override def windowClosing(e: WindowEvent): Unit = {
            callback(RightUnit)
          }
        }
      )
    }

  def start: F[F[Unit]] = {
    for {
      model <- makeModel
      close <- Swing[F].onUI {
        val view = makeView(model)
        view.setVisible(true)
        registerClosing(view)
      }
    } yield close
  }

}
object Gui {

  def make[F[+_]: Async: Swing](notificationService: NotificationService[F]): F[Gui[F]] =
    new Gui[F](notificationService).pure[F]

  final val RightUnit: Either[Throwable, Unit] = Right(())

}