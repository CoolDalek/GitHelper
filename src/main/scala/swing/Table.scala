package swing

import cats.syntax.all.*
import cats.{Applicative, MonadThrow}

import scala.reflect.ClassTag

trait Table[T]:

  def isEmpty: Boolean = columns == 0

  def nonEmpty: Boolean = columns != 0

  def apply(i: Int, j: Int): T

  def row(i: Int): IArray[T]

  def column(i: Int): IArray[T]

  def rows: Int

  def columns: Int

  def columnName(i: Int): String =
    val result = new StringBuilder
    var counter = i
    while(counter >= 0) {
      result += ((counter % 26).asInstanceOf[Char] + 'A').asInstanceOf[Char]
      counter = counter/26 - 1
    }
    result.reverse.result
  end columnName

object Table:

  private trait NamedTable[T] extends Table[T]:

    protected def getColumnName(i: Int, dict: AnyRef)
                               (get: => String): String =
      if(dict != null)
        if(i < columns) get
        else noColumnWithIndex(i)
      else super.columnName(i)

  end NamedTable

  private trait HasName[T] extends NamedTable[T]:
    def name: String
    override def columnName(i: Int): String =
      getColumnName(i, name)(name)
  end HasName

  private trait HasNames[T] extends NamedTable[T]:
    def names: IArray[String]
    override def columnName(i: Int): String =
      getColumnName(i, names.asInstanceOf[AnyRef])(names(i))
  end HasNames

  private class TableImpl[T: ClassTag](
                                        underlying: IArray[IArray[T]],
                                        val names: IArray[String],
                                      ) extends HasNames[T]:
    
    override def row(i: Int): IArray[T] =
      for {
        column <- underlying
      } yield column(i)

    override def column(i: Int): IArray[T] = underlying(i)

    override def apply(i: Int, j: Int): T =
      if(i < columns && j < rows) {
        underlying(i)(j)
      } else noElemWithIndex(i, j)

    override def rows: Int =
      if(nonEmpty) {
        underlying(0).length
      } else 0

    override def columns: Int = underlying.length

    override def toString: String =
      val rows = underlying.map { rows =>
        rows.map { row =>
          s"\n\t\t$row"
        }
      }
      val columns = rows.map(_.mkString("\t(", ";", "\n\t);\n"))
      val table = columns.mkString("(\n", ",\n", ")")
      s"Table$table"
    end toString

  end TableImpl

  private class OneColumn[T: ClassTag](
                                        underlying: IArray[T], 
                                        val name: String,
                                      ) extends HasName[T]:

    override def apply(i: Int, j: Int): T =
      if(i == 0) underlying(j)
      else noColumnWithIndex(i)
    
    override def row(i: Int): IArray[T] = IArray(underlying(i))

    override def column(i: Int): IArray[T] =
      if(i == 0) underlying
      else noColumnWithIndex(i)

    override def rows: Int = underlying.length

    override val columns: Int = 1

    override def toString: String =
      val tabulated = underlying.map { elem =>
        s"\t$elem"
      }
      val table = tabulated.mkString("(\n", ";\n", ")")
      s"table$table"
    end toString

  end OneColumn

  private class OneElement[T: ClassTag](
                                         underlying: T,
                                         val name: String,
                                       ) extends HasName[T]:

    override def row(i: Int): IArray[T] =
      if(i == 0) IArray(underlying)
      else noRowWithIndex(i)

    override def column(i: Int): IArray[T] =
      if(i == 0) IArray(underlying)
      else noColumnWithIndex(i)

    override def apply(i: Int, j: Int): T =
      if(i < 1 && j < 1) underlying
      else noElemWithIndex(i, j)

    override val rows: Int = 1

    override val columns: Int = 1

    override def toString: String = s"Table($underlying)"

  end OneElement

  private def noElemWithIndex(i: Int, j: Int): Nothing =
    throw new NoSuchElementException(s"There is no element with index {$i,$j}.")

  private def noColumnWithIndex(i: Int): Nothing =
    throw new NoSuchElementException(s"There is no column with index $i.")

  private def noRowWithIndex(i: Int): Nothing =
    throw new NoSuchElementException(s"There is no row with index $i.")

  private def aligned[F[+_] : MonadThrow, T](underlying: IArray[IArray[T]],
                                             names: IArray[String])
                                            (body: => Table[T]): F[Table[T]] = {
    val validateNames =
      if(names ne null) {
        val correct = names.length == underlying.length
        if(correct) {
          MonadThrow[F].unit
        } else {
          NamesCountException(
            expected = underlying.length,
            real = names.length,
          ).raiseError[F, Table[T]]
        }
      } else {
        MonadThrow[F].unit
      }
    val validateAlignment = if(underlying.nonEmpty) {
      val length = underlying.map(_.length)
      val correct = length.tail.forall(_ == length.head)
      if(correct) {
        MonadThrow[F].unit
      } else {
        TableMustBeAligned.raiseError[F, Table[T]]
      }
    } else {
      MonadThrow[F].unit
    }
    for {
      _ <- validateNames
      _ <- validateAlignment
    } yield body
  }

  private inline def NullString = null.asInstanceOf[String]

  private inline def NullStringIArray = null.asInstanceOf[IArray[String]]

  def column[F[+_]: Applicative, T: ClassTag](underlying: IArray[T]): F[Table[T]] =
    new OneColumn[T](underlying, NullString).pure[F]

  def column[F[+_]: Applicative, T: ClassTag](underlying: IArray[T], columnName: String): F[Table[T]] =
    new OneColumn[T](underlying, columnName).pure[F]

  def one[F[+_]: Applicative, T: ClassTag](underlying: T): F[Table[T]] =
    new OneElement[T](underlying, NullString).pure[F]

  def one[F[+_]: Applicative, T: ClassTag](underlying: T, columnName: String): F[Table[T]] =
    new OneElement[T](underlying, columnName).pure[F]

  def apply[F[+_]: MonadThrow, T: ClassTag](underlying: IArray[IArray[T]]): F[Table[T]] =
    aligned[F, T](underlying, NullStringIArray) {
      new TableImpl(underlying, NullStringIArray)
    }

  def apply[F[+_]: MonadThrow, T: ClassTag](underlying: IArray[IArray[T]], columnNames: IArray[String]): F[Table[T]] =
    aligned[F, T](underlying, columnNames) {
      new TableImpl(underlying, columnNames)
    }

  case object TableMustBeAligned extends Exception("Table must be aligned")

  case class NamesCountException(expected: Int,
                                 real: Int) extends Exception(
    s"Names count exception, expected: $expected, real: $real."
  )

end Table