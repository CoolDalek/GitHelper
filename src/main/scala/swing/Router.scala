package swing

import cats.effect.std.Semaphore
import cats.effect.{Concurrent, Deferred, Spawn}
import cats.syntax.all._
import cats.{Applicative, Defer}
import swing.Router.Route
import swing.Screen.ScreenView

import java.util.{Objects, UUID}
import javax.swing.JFrame
import scala.collection.concurrent.TrieMap

trait Router[F[_]] {

  private[swing] def requestBuild(route: Route): F[Option[F[ScreenView]]]

  private[swing] def commitBuild(route: Route, value: F[ScreenView]): F[Unit]

  def request(route: Route): F[ScreenView]

  def move(from: Route, to: Route): F[Unit]

  private[swing] def initialScreen(route: Route): F[Unit]

}
object Router {

  private class RouterImpl[F[_]: Swing: Concurrent](window: JFrame) extends Router[F] {

    sealed trait Cell {

      def request: F[ScreenView]

    }
    class Uninitialized(
                         private val lock: Semaphore[F],
                         private val view: Deferred[F, ScreenView],
                         @volatile private var waitView: Boolean,
                       ) extends Cell {

      def acquire: F[Unit] = lock.acquire

      def release: F[Unit] = lock.release

      override def request: F[ScreenView] = {
        waitView = true
        view.get
      }

      def commit(value: F[ScreenView]): F[Initialized] = {
        val committed = for {
          lock <- Cell.semaphore
        } yield new Initialized(
          lock = lock,
          value = value,
          view = view,
        )
        if(waitView) {
          committed.flatTap(_.request)
        } else {
          committed
        }
      }

    }
    class Initialized(
                       private val lock: Semaphore[F],
                       val value: F[ScreenView],
                       private val view: Deferred[F, ScreenView],
                     ) extends Cell {

      def dispose: F[Initialized] = for {
        empty <- Cell.deferred
      } yield new Initialized(
        lock,
        value,
        empty,
      )

      override def request: F[ScreenView] = {
        val screenView = for {
          _ <- lock.acquire
          maybeScreen <- view.tryGet
          screen <- maybeScreen match {
            case Some(value) =>
              value.pure[F]
            case None =>
              value.flatTap(view.complete)
          }
          _ <- lock.release
        } yield screen
        screenView
      }
    }
    object Cell {

      def semaphore: F[Semaphore[F]] = Semaphore[F](1)

      def deferred: F[Deferred[F, ScreenView]] = Deferred[F, ScreenView]

      def apply(): F[Cell] =
        for {
          lock <- semaphore
          view <- deferred
        } yield new Uninitialized(
          lock = lock,
          view = view,
          waitView = false,
        )

    }

    private val buildMap = TrieMap.empty[String, Cell]

    private[swing] def requestBuild(route: Route): F[Option[F[ScreenView]]] =
      Defer[F].defer {
        buildMap.get(route.id) match {
          case Some(value) =>
            value match {
              case cell: Uninitialized =>
                for {
                  _ <- cell.acquire
                  screen <- buildMap(route.id) match {
                    case _: Uninitialized => None.pure[F]
                    case changed: Initialized =>
                      cell.release as Some(changed.value)
                  }
                } yield screen
              case cell: Initialized =>
                Option(cell.value).pure[F]
            }
          case None =>
            loop(route, requestBuild)
        }
      }

    private[swing] def commitBuild(route: Route, value: F[ScreenView]): F[Unit] =
      Defer[F].defer {
        buildMap(route.id) match {
          case cell: Uninitialized =>
            for {
              committed <- cell.commit(value)
              _ = buildMap.update(route.id, committed)
              _ <- cell.release
            } yield ()
          case _: Initialized =>
            Applicative[F].unit
        }
      }

    private[swing] def dispose(route: Route): F[Unit] =
      Defer[F].defer {
        buildMap(route.id) match {
          case _: Uninitialized =>
            Applicative[F].unit
          case cached: Initialized =>
            for {
              disposed <- cached.dispose
            } yield buildMap.update(route.id, disposed)
        }
      }

    def request(route: Route): F[ScreenView] = Defer[F].defer {
      buildMap.get(route.id) match {
        case Some(cell) =>
          cell.request
        case None =>
          loop(route, request)
      }
    }

    private def transition(before: F[Unit], route: Route): F[Unit] =
      Spawn[F].start(
        for {
          _ <- before
          screen <- request(route)
          _ <- Swing[F].onUI {
            window.setContentPane(screen)
            window.revalidate()
          }
        } yield ()
      ).void

    private[swing] def initialScreen(route: Route): F[Unit] =
      transition(Applicative[F].unit, route)

    def move(from: Route, to: Route): F[Unit] =
      transition(dispose(from), to)

    private def loop[T](route: Route, recursive: Route => F[T]): F[T] =
      for {
        cell <- Cell()
        _ = buildMap.putIfAbsent(route.id, cell)
        result <- recursive(route)
      } yield result

  }

  class Route private[swing]() {

    private[swing] val id: String = UUID.randomUUID().toString

    override def equals(obj: Any): Boolean =
      if(Objects.nonNull(obj)) {
        obj match {
          case that: Route => that.id == this.id
          case _ => false
        }
      } else false

  }

  def apply[F[_]: Swing: Concurrent](window: JFrame): Router[F] = new RouterImpl[F](window)

}