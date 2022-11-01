package provider

import effects.*

trait CryptoProvider[F[_]]:

  def encrypt(data: Array[Byte]): F[Array[Byte]]

  def decrypt(data: Array[Byte]): F[Array[Byte]]

  def encrypt(data: String, charset: String): F[String]

  def decrypt(data: String, charset: String): F[String]

  def encryptBytes[T: PrepareEncryption](data: T): F[Array[Byte]]

  def decryptBytes[T: PrepareDecryption](data: Array[Byte]): F[T]

  def encryptString[T: PrepareEncryption](data: T): F[String]

  def decryptString[T: PrepareDecryption](data: String): F[T]

end CryptoProvider
