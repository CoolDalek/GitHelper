package service.impl

import cats.Traverse
import cats.effect.Temporal
import cats.syntax.all._
import config.NotificationConfig
import dao.TokenDao
import fs2.Stream
import integration.GithubClient
import model.Exceptions.NoTokenProvided
import model.{ApiToken, Notification}
import service.NotificationService

class NotificationServiceImpl[F[_]: Temporal](
                                               config: NotificationConfig,
                                               client: GithubClient[F],
                                               tokenDao: TokenDao[F],
                                             ) extends NotificationService[F] {

  override def notifications: Stream[F, Seq[Notification]] =
    Stream.awakeEvery(config.pollingDelay).evalMap { _ =>
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
    }

}
object NotificationServiceImpl {

  def make[F[+_]: Temporal](
                             config: NotificationConfig,
                             client: GithubClient[F],
                             tokenDao: TokenDao[F],
                           ): F[NotificationService[F]] =
    new NotificationServiceImpl[F](config, client, tokenDao).pure[F]

}