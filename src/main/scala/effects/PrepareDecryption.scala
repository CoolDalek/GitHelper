package effects

trait PrepareDecryption[T] {

  def charset: String

  def fromString(decodedString: String): T

  def fromBytes(decodedBytes: Array[Byte]): T

}
object PrepareDecryption extends CryptoCompanion[PrepareDecryption] {

  def fromString[T](charSet: String)(deserialize: String => T): PrepareDecryption[T] =
    new PrepareDecryption[T] {

      override val charset: String = charSet

      override def fromString(decodedString: String): T = deserialize(decodedString)

      override def fromBytes(decodedBytes: Array[Byte]): T = fromString(new String(decodedBytes, charset))

    }

  def fromString[T](deserialize: String => T): PrepareDecryption[T] =
    fromString(Utf8)(deserialize)

  def fromBytes[T](charSet: String)(deserialize: Array[Byte] => T): PrepareDecryption[T] =
    new PrepareDecryption[T] {
      override val charset: String = charSet

      override def fromString(decodedString: String): T = fromBytes(decodedString.getBytes(charset))

      override def fromBytes(decodedBytes: Array[Byte]): T = deserialize(decodedBytes)
    }

  def fromBytes[T](deserialize: Array[Byte] => T): PrepareDecryption[T] =
    fromBytes(Utf8)(deserialize)

}