package service.impl

import cats.*
import cats.effect.*
import cats.syntax.all.*
import config.NotificationConfig
import dao.TokenDao
import fs2.Stream
import integration.GithubClient
import model.*
import service.NotificationService

class NotificationServiceImpl[F[_]: Temporal](
                                               config: NotificationConfig,
                                               client: GithubClient[F],
                                               tokenDao: TokenDao[F],
                                             ) extends NotificationService[F]:

  private def fetchNotifications: F[Seq[Notification]] =
    for {
      token <- tokenDao.getToken.getOrElseF {
        NoTokenProvided.raiseError[F, ApiToken]
      }
      repos <- client.repositories(token)
      notifications = repos.map { repo =>
        for {
          pulls <- client.pullRequests(repo, token)
        } yield Notification(
          repoName = repo.name,
          repoUrl = repo.htmlUrl,
          pullUrls = pulls.map(_.htmlUrl)
        )
      }
      result <- Traverse[Seq].sequence(notifications)
    } yield result
  end fetchNotifications

  override def remote: Stream[F, Seq[Notification]] =
    Stream.eval(fetchNotifications).append {
      Stream.awakeEvery(config.pollingDelay)
        .evalMap(_ => fetchNotifications)
    }

  override def local: F[Seq[Notification]] = Seq.empty[Notification].pure[F]

  override def safe(seq: Seq[Notification]): F[Unit] = Applicative[F].unit

object NotificationServiceImpl:

  def make[F[+_]: Temporal](
                             config: NotificationConfig,
                             client: GithubClient[F],
                             tokenDao: TokenDao[F],
                           ): F[NotificationService[F]] =
    new NotificationServiceImpl[F](config, client, tokenDao).pure[F]

end NotificationServiceImpl
