package swing

import cats.effect.Resource

import javax.swing.{JComponent, JFrame}

sealed trait ViewComponent:
  type View

trait Window:
  this: ViewComponent =>
  override type View = JFrame

type WindowComponent = Window & ViewComponent

trait Screen:
  this: ViewComponent =>
  override type View = JComponent

type ScreenComponent = Screen & ViewComponent

trait Stateless extends ViewComponent:
  def view: View

trait Stateful[F[_]] extends ViewComponent:
  type Model
  def model: Resource[F, Model]
  def view(model: Model): View
