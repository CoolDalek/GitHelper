package service

import fs2.Stream
import model.Notification

trait NotificationService[F[_]]:
  
  def local: F[Seq[Notification]]
  
  def remote: Stream[F, Seq[Notification]]
  
  def safe(seq: Seq[Notification]): F[Unit]

end NotificationService
