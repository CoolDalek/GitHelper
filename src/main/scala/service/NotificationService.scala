package service
import fs2.Stream
import model.Notification

trait NotificationService[F[_]] {

  def notifications: Stream[F, Seq[Notification]]

}