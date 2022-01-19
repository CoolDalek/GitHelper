package model

import cats.Show
import cats.effect.Spawn
import cats.syntax.all._
import effects.Swing
import fs2.{Compiler, Stream}

import javax.swing.table.AbstractTableModel

class SwingTableModel[F[_]: Swing, T: Show](
                                             initial: Table[T],
                                           ) extends AbstractTableModel {
  @volatile private var table: Table[T] = initial

  private def register(source: Stream[F, Table[T]],
                       updateHeaders: Boolean)
                      (implicit compiler: Compiler[F, F],
                       spawn: Spawn[F]): F[SwingTableModel[F, T]] = {
    val updating = source.foreach { updated =>
      Swing[F].onUI {
        table = updated
        if(updateHeaders) {
          fireTableStructureChanged()
        } else {
          fireTableDataChanged()
        }
      }
    }.compile.drain
    spawn.start(updating).as(this)
  }

  override def getColumnClass(columnIndex: Int): Class[_] = classOf[String]

  override def getRowCount: Int =
    table.rows

  override def getColumnCount: Int =
    table.columns

  override def getValueAt(rowIndex: Int, columnIndex: Int): String =
    table(columnIndex, rowIndex).show

  override def getColumnName(column: Int): String =
    table.columnName(column)

}
object SwingTableModel {

  def make[F[_]: Swing: Spawn, T: Show](source: Stream[F, Table[T]],
                                        initial: Table[T],
                                        updateHeaders: Boolean = false)
                                       (implicit compiler: Compiler[F, F]): F[SwingTableModel[F, T]] =
    new SwingTableModel[F, T](initial).register(source, updateHeaders)

}