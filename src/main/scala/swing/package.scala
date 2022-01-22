package object swing {

  type AwtListener = java.awt.event.ActionListener

  final val RightUnit: Either[Throwable, Unit] = Right(())

  implicit class ListenerSyntax[T: ActionListener](private val self: T) {

    def addListener(listener: AwtListener): Unit =
      ActionListener[T].add(self)(listener)

    def removeListener(listener: AwtListener): Unit =
      ActionListener[T].remove(self)(listener)

  }

}