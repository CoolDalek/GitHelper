package swing

import cats.Show
import cats.effect.{IO, Resource, Spawn}
import cats.syntax.all.*
import fs2.{Compiler, Stream}

import javax.swing.table.AbstractTableModel

class SwingTableModel[T: Show](
                                initial: Table[T],
                              ) extends AbstractTableModel:
  private var table: Table[T] = initial

  override def getColumnClass(columnIndex: Int): Class[?] = classOf[String]

  override def getRowCount: Int =
    table.rows

  override def getColumnCount: Int =
    table.columns

  override def getValueAt(rowIndex: Int, columnIndex: Int): String =
    table(columnIndex, rowIndex).show

  override def getColumnName(column: Int): String =
    table.columnName(column)

  def getTable: Table[T] = table

object SwingTableModel:

  def make[F[_]: UIConsumer, T: Show](
                                       source: Stream[F, Table[T]],
                                       initial: Table[T],
                                       updateHeaders: Boolean = false
                                     ): (SwingTableModel[T], Resource[F, Unit]) = {
    val model = new SwingTableModel(initial)
    model -> source.forkWithEffect { updated =>
      model.table = updated
      if (updateHeaders) model.fireTableStructureChanged()
      else model.fireTableDataChanged()
    }
  }

end SwingTableModel