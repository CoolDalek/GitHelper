package gui

import cats.*
import cats.effect.*
import cats.syntax.all.*
import model.Notification
import service.NotificationService
import swing.*

import javax.swing.{JComponent, JScrollPane, JTable}

class NotificationScreen[F[+_]: UIConsumer: Monad](
                                                    notificationService: NotificationService[F],
                                                  ) extends Screen, Stateful[F] {
  override type Model = SwingTableModel[Notification]
  override type View = JComponent

  override def model: Resource[F, Model] =
    def unpickle(seq: Seq[Notification]): F[Table[Notification]] =
      Table.column(IArray.from(seq))

    for {
      init <- Resource.eval {
        notificationService.local.flatMap(unpickle)
      }
      updates = notificationService.remote
        .evalMap(unpickle)
      (model, sub) = SwingTableModel.make(updates, init)
      _ <- sub.onFinalize {
        notificationService.safe(
          model.getTable.column(0)
        )
      }
    } yield model
  end model

  override def view(model: Model): View =
    new JScrollPane(
      new JTable(model) {
        setFillsViewportHeight(true)
      }
    )

}
