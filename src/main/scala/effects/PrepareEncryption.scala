package effects

import cats.Show

trait PrepareEncryption[T] {

  def charset: String

  def asString(obj: T): String

  def asBytes(obj: T): Array[Byte]

}
object PrepareEncryption extends CryptoCompanion[PrepareEncryption] {

  def asString[T](charSet: String)(serialize: T => String): PrepareEncryption[T] =
    new PrepareEncryption[T] {

      override val charset: String = charSet

      override def asString(obj: T): String = serialize(obj)

      override def asBytes(obj: T): Array[Byte] = asString(obj).getBytes(charset)

    }

  def asString[T](serialize: T => String): PrepareEncryption[T] =
    asString(Utf8)(serialize)

  def asBytes[T](charSet: String)(serialize: T => Array[Byte]): PrepareEncryption[T] =
    new PrepareEncryption[T] {

      override val charset: String = charSet

      override def asString(obj: T): String = new String(asBytes(obj), charset)

      override def asBytes(obj: T): Array[Byte] = serialize(obj)

    }

  def asBytes[T](serialize: T => Array[Byte]): PrepareEncryption[T] =
    asBytes(Utf8)(serialize)

  def show[T: Show]: PrepareEncryption[T] = asString(Show[T].show)

}