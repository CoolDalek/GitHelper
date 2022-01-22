package swing

import cats.syntax.all._
import cats.{Applicative, MonadThrow}

import java.util.Objects
import scala.reflect.ClassTag

trait Table[T] {

  def isEmpty: Boolean = columns == 0

  def nonEmpty: Boolean = columns != 0

  def apply(i: Int, j: Int): T

  def rows: Int

  def columns: Int

  def columnName(i: Int): String = {
    val result = new StringBuilder
    var counter = i
    while(counter >= 0) {
      result += ((counter % 26).asInstanceOf[Char] + 'A').asInstanceOf[Char]
      counter = counter/26 - 1
    }
    result.reverse.result
  }

}
object Table {

  private trait NamedTable[T] extends Table[T] {

    protected def getColumnName(i: Int, dict: AnyRef)
                               (get: => String): String =
      if(Objects.nonNull(dict)) {
        if(i < columns) get
        else noColumnWithIndex(i)
      } else {
        super.columnName(i)
      }

  }

  private trait HasName[T] extends NamedTable[T] {
    def name: String
    override def columnName(i: Int): String =
      getColumnName(i, name)(name)
  }

  private trait HasNames[T] extends NamedTable[T] {
    def names: Array[String]
    override def columnName(i: Int): String =
      getColumnName(i, names)(names(i))
  }

  private class TableImpl[T](
                              underlying: Array[Array[T]],
                              val names: Array[String],
                            )
    extends HasNames[T] {

    def apply(i: Int, j: Int): T =
      if(i < columns && j < rows) {
        underlying(i)(j)
      } else noElemWithIndex(i, j)

    def rows: Int =
      if(nonEmpty) {
        underlying(0).length
      } else 0

    def columns: Int = underlying.length

    override def toString: String = {
      val rows = underlying.map { rows =>
        rows.map { row =>
          s"\n\t\t$row"
        }
      }
      val columns = rows.map(_.mkString("\t(", ";", "\n\t);\n"))
      val table = columns.mkString("(\n", ",\n", ")")
      s"Table$table"
    }

  }

  private class OneColumn[T](
                              underlying: Array[T],
                              val name: String,
                            ) extends HasName[T] {

    override def apply(i: Int, j: Int): T =
      if(i == 0) underlying(j)
      else noColumnWithIndex(i)

    override def rows: Int = underlying.length

    override val columns: Int = 1

    override def toString: String = {
      val tabulated = underlying.map { elem =>
        s"\t$elem"
      }
      val table = tabulated.mkString("(\n", ";\n", ")")
      s"table$table"
    }

  }

  private class OneElement[@specialized T](
                                            underlying: T,
                                            val name: String,
                                          ) extends HasName[T] {

    override def apply(i: Int, j: Int): T =
      if(i < 1 && j < 1) underlying
      else noElemWithIndex(i, j)

    override val rows: Int = 1

    override val columns: Int = 1

    override def toString: String = s"Table($underlying)"

  }

  private def noElemWithIndex(i: Int, j: Int): Nothing =
    throw new NoSuchElementException(s"There is no element with index {$i,$j}.")

  private def noColumnWithIndex(i: Int): Nothing =
    throw new NoSuchElementException(s"There is no column with index $i.")

  private def aligned[F[+_] : MonadThrow, T](underlying: Array[Array[T]],
                                             names: Array[String])
                                            (body: => Table[T]): F[Table[T]] = {
    val validateNames =
      if(Objects.nonNull(names)) {
        val correct = names.length == underlying.length
        if(correct) {
          Applicative[F].unit
        } else {
          NamesCountException(
            expected = underlying.length,
            real = names.length,
          ).raiseError[F, Table[T]]
        }
      } else {
        Applicative[F].unit
      }
    val validateAlignment = if(underlying.nonEmpty) {
      val length = underlying.map(_.length)
      val correct = length.tail.forall(_ == length.head)
      if(correct) {
        Applicative[F].unit
      } else {
        TableMustBeAligned.raiseError[F, Table[T]]
      }
    } else {
      Applicative[F].unit
    }
    for {
      _ <- validateNames
      _ <- validateAlignment
    } yield body
  }

  private val NullString = null.asInstanceOf[String]

  private val NullStringArray = null.asInstanceOf[Array[String]]

  def column[F[+_]: Applicative, T: ClassTag](underlying: Array[T]): F[Table[T]] =
    new OneColumn[T](underlying, NullString).pure[F]

  def column[F[+_]: Applicative, T: ClassTag](underlying: Array[T], columnName: String): F[Table[T]] =
    new OneColumn[T](underlying, columnName).pure[F]

  def one[F[+_]: Applicative, T: ClassTag](underlying: T): F[Table[T]] =
    new OneElement[T](underlying, NullString).pure[F]

  def one[F[+_]: Applicative, T: ClassTag](underlying: T, columnName: String): F[Table[T]] =
    new OneElement[T](underlying, columnName).pure[F]

  def apply[F[+_]: MonadThrow, T](underlying: Array[Array[T]]): F[Table[T]] =
    aligned[F, T](underlying, NullStringArray) {
      new TableImpl(underlying, NullStringArray)
    }

  def apply[F[+_]: MonadThrow, T](underlying: Array[Array[T]], columnNames: Array[String]): F[Table[T]] =
    aligned[F, T](underlying, columnNames) {
      new TableImpl(underlying, columnNames)
    }

  case object TableMustBeAligned extends Exception("Table must be aligned")

  case class NamesCountException(expected: Int,
                                 real: Int) extends Exception(
    s"Names count exception, expected: $expected, real: $real."
  )

}