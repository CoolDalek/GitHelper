package swing

import cats.*
import cats.effect.*
import cats.effect.std.*
import cats.syntax.all.*
import effects.SummonerK

import java.awt.event.{WindowAdapter, WindowEvent}

trait Router[F[_]]:

  def moveToScreen[T <: ScreenComponent](route: Delayed[F, T]): Unit

  def moveToWindow[T <: WindowComponent](route: Delayed[F, T]): F[Unit]

object Router extends SummonerK[Router]:

  private class RouterImpl[F[_]: Async: Swing](
                                                dispatcher: Dispatcher[F],
                                                hotswap: Hotswap[F, Any],
                                              ) extends Router[F]:

    private def evalView[T <: ViewComponent](component: T): F[component.View] =
      Async[F].defer {
        val view = component match
          case stateless: Stateless =>
            Swing[F].onUI(stateless.view)
          case stateful: Stateful[F] =>
            hotswap.swap {
              stateful.model.evalMap { model =>
                Swing[F].onUI(stateful.view(model))
              }
            }
        view.asInstanceOf[F[component.View]]
      }
    end evalView

    private def getComponent[T <: ViewComponent](route: Delayed[F, T]): T =
      dispatcher.unsafeRunSync(route.get)

    override def moveToScreen[T <: ScreenComponent](route: Delayed[F, T]): Unit =
      val component = getComponent(route)
      val view = evalView(component)
      dispatcher.unsafeRunAndForget(view)
    end moveToScreen

    override def moveToWindow[T <: WindowComponent](route: Delayed[F, T]): F[Unit] =
      for {
        component <- route.get
        window <- evalView(component)
        _ <- Swing[F].fromUI[Unit] { cb =>
          window.addWindowListener {
            new WindowAdapter {
              override def windowClosing(e: WindowEvent): Unit = cb(Right(()))
            }
          }
        }
      } yield ()
    end moveToWindow

  end RouterImpl

  def make[F[_]: Async: Swing]: Resource[F, Router[F]] =
    for {
      dispatcher <- Dispatcher[F]
      hotswap <- Hotswap.create[F, Any]
    } yield RouterImpl[F](
      dispatcher,
      hotswap,
    )

end Router