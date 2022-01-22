package swing

import effects.Summoner

import javax.swing.JButton

trait ActionListener[T] {

  def add(self: T)(listener: AwtListener): Unit

  def remove(self: T)(listener: AwtListener): Unit

}
object ActionListener extends Summoner[ActionListener] {

  implicit val jButtonListener: ActionListener[JButton] = new ActionListener[JButton] {

    override def add(self: JButton)(listener: AwtListener): Unit =
      self.addActionListener(listener)

    override def remove(self: JButton)(listener: AwtListener): Unit =
      self.removeActionListener(listener)

  }

}