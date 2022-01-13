package effects

trait PrepareDecription[T] {

  def charset: String

  def fromString(decodedString: String): T

  def fromBytes(decodedBytes: Array[Byte]): T

}
object PrepareDecription extends CryptoCompanion[PrepareDecription] {

  def fromString[T](charSet: String)(deserialize: String => T): PrepareDecription[T] =
    new PrepareDecription[T] {

      override val charset: String = charSet

      override def fromString(decodedString: String): T = deserialize(decodedString)

      override def fromBytes(decodedBytes: Array[Byte]): T = fromString(new String(decodedBytes, charset))

    }

  def fromString[T](deserialize: String => T): PrepareDecription[T] =
    fromString(Utf8)(deserialize)

  def fromBytes[T](charSet: String)(deserialize: Array[Byte] => T): PrepareDecription[T] =
    new PrepareDecription[T] {
      override val charset: String = charSet

      override def fromString(decodedString: String): T = fromBytes(decodedString.getBytes(charset))

      override def fromBytes(decodedBytes: Array[Byte]): T = deserialize(decodedBytes)
    }

  def fromBytes[T](deserialize: Array[Byte] => T): PrepareDecription[T] =
    fromBytes(Utf8)(deserialize)

}