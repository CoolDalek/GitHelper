package swing

import cats.effect.{Fiber, Spawn}
import cats.syntax.all._
import cats.{Applicative, Defer, Traverse}

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec
import scala.util.control.NonFatal

abstract class UIComponent[F[_]: Swing: Spawn] {
  type Model
  type View
  type Built

  private sealed trait State
  private case class Initializing(eventHandlers: Vector[F[Unit]]) extends State {
    def addHandler(handler: F[Unit]): Initializing = copy(
      eventHandlers = eventHandlers.appended(handler)
    )
    def froze: Frozen = Frozen(eventHandlers)
  }
  private object Initializing {
    val empty: Initializing = Initializing(Vector.empty)
  }
  private case class Frozen(eventHandlers: Vector[F[Unit]]) extends State
  private case class Active(eventServer: Fiber[F, Throwable, Unit]) extends State
  private case object Inactive extends State

  private val state = new AtomicReference[State](Initializing.empty)

  private[swing] final def prepare: F[Unit] =
    Swing[F].suspend {
      state.compareAndSet(Inactive, Initializing.empty)
    }.void

  protected final def actionListener[T: ActionListener](on: T)(action: => F[Unit]): Unit = {
    def makeHandler: F[Unit] = {
      val setListener =
        Swing[F].fromUI[Unit] { callback =>
          on.addListener { _ =>
            callback(RightUnit)
          }
        }
      val performAction = setListener >> action
      performAction.onError {
        case NonFatal(exc) =>
          Swing[F].suspend(println(exc))
      }
    }

    @tailrec
    def casLoop(nullable: F[Unit]): Unit =
      state.get match {
        case current: Initializing =>
          val handler = if(nullable == null) {
            makeHandler
          } else nullable
          val updated = current.addHandler(handler)
          val success = state.compareAndSet(current, updated)
          if(!success) {
            casLoop(handler)
          }
        case illegalState =>
          throw new IllegalStateException(s"Cannot add action listener in $illegalState state.")
      }
    casLoop(null.asInstanceOf[F[Unit]])
  }

  private[swing] final def handleEvents: F[Unit] = {
    def casLoop: F[Unit] =
      state.get match {
        case init: Initializing =>
          state.compareAndSet(init, init.froze)
          casLoop
        case frozen: Frozen =>
          val batch = Traverse[Vector]
            .sequence(frozen.eventHandlers)
            .void
          for {
            fiber <- Spawn[F].start(batch)
            active = Active(fiber)
            success = state.compareAndSet(frozen, active)
            _ <- if(success) {
              Applicative[F].unit
            } else for {
              _ <- fiber.cancel
              _ <- casLoop
            } yield ()
          } yield ()
        case _ =>
          Applicative[F].unit
      }
    Defer[F].defer(casLoop)
  }

  private[swing] final def cleanup: F[Unit] = {
    @tailrec
    def casLoop: F[Unit] =
      state.get() match {
        case active: Active =>
          val success = state.compareAndSet(active, Inactive)
          if (success) {
            active.eventServer.cancel
            Applicative[F].unit
          } else {
            casLoop
          }
        case _ =>
          Applicative[F].unit
      }
    Defer[F].defer(casLoop)
  }

  def makeModel: F[Model]

  def makeView(model: Model): F[View] = Swing[F].onUI(view(model))

  protected def view(model: Model): View

  def build: F[Built]

}