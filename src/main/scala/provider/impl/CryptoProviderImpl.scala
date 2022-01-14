package provider.impl

import cats.MonadThrow
import cats.effect.kernel.Sync
import cats.effect.std.Random
import cats.syntax.all._
import config.CryptoConfig
import effects.{PrepareEncryption, PrepareDecryption}
import provider.CryptoProvider
import provider.impl.CryptoProviderImpl._

import java.util.Base64
import javax.crypto.spec.{IvParameterSpec, PBEKeySpec, SecretKeySpec}
import javax.crypto.{Cipher, SecretKey, SecretKeyFactory}
import scala.util.control.NonFatal

class CryptoProviderImpl[F[_]: MonadThrow](
                                           config: CryptoConfig,
                                           random: Random[F],
                                           encoder: Base64.Encoder,
                                           decoder: Base64.Decoder,
                                         ) extends CryptoProvider[F] {
  import config._

  def generateKey(salt: Array[Byte]): SecretKey = {
    val factory = SecretKeyFactory.getInstance(PBKBF2Algorithm)
    val spec = new PBEKeySpec(password.toCharArray, salt, KeyIterations, KeyLength)
    val underlying = factory.generateSecret(spec)
    new SecretKeySpec(underlying.getEncoded, AesAlgorithm)
  }

  def generateIV: F[IvParameterSpec] =
    random.nextBytes(IvLength)
      .map(iv => new IvParameterSpec(iv))

  def generateSalt: F[Array[Byte]] =
    random.nextBytes(SaltLength)

  def cipher: Cipher = Cipher.getInstance(AesTransformation)

  def encrypt(data: Array[Byte]): F[Array[Byte]] =
    for {
      salt <- generateSalt
      key = generateKey(salt)
      iv <- generateIV
      encipher = cipher
      _ = encipher.init(Cipher.ENCRYPT_MODE, key, iv)
      encrypted = encipher.doFinal(data)
      packed = salt ++ iv.getIV ++ encrypted
    } yield encoder.encode(packed)

  override def encrypt(data: String, charset: String): F[String] =
    encrypt(data.getBytes(charset))
      .map(x => new String(x, charset))

  case class Unpacked(salt: Array[Byte], iv: IvParameterSpec, encrypted: Array[Byte])

  def unpack(data: Array[Byte]): F[Unpacked] =
    try {
      val packed = decoder.decode(data)
      val salt = packed.take(SaltLength)
      val iv = new IvParameterSpec(
        packed.slice(SaltLength, DataPosition)
      )
      val encrypted = packed.drop(DataPosition)
      Unpacked(salt, iv, encrypted).pure[F]
    } catch {
      case NonFatal(exc) =>
        exc.raiseError[F, Unpacked]
    }

  def decrypt(data: Array[Byte]): F[Array[Byte]] =
    for {
      unpacked <- unpack(data)
      decipher = cipher
      key = generateKey(unpacked.salt)
      _ = decipher.init(Cipher.DECRYPT_MODE, key, unpacked.iv)
    } yield decipher.doFinal(unpacked.encrypted)

  override def decrypt(data: String, charset: String): F[String] =
    decrypt(data.getBytes(charset))
      .map(x => new String(x, charset))

  override def encryptBytes[T: PrepareEncryption](data: T): F[Array[Byte]] =
    encrypt(PrepareEncryption[T].asBytes(data))

  override def decryptBytes[T: PrepareDecryption](data: Array[Byte]): F[T] =
    decrypt(data).map(x => PrepareDecryption[T].fromBytes(x))

  override def encryptString[T: PrepareEncryption](data: T): F[String] = {
    val write = PrepareEncryption[T]
    val string = write.asString(data)
    encrypt(string, write.charset)
  }

  override def decryptString[T: PrepareDecryption](data: String): F[T] = {
    val read = PrepareDecryption[T]
    decrypt(data, read.charset)
      .map(x => read.fromString(x))
  }

}
object CryptoProviderImpl {

  private final val AesAlgorithm = "AES"

  private final val AesTransformation = s"$AesAlgorithm/CTR/NOPADDING"

  private final val PBKBF2Algorithm = "PBKDF2WithHmacSHA256"

  private final val KeyIterations = 65536

  private final val KeyLength = 256 // bits

  private final val SaltLength = KeyLength / 8 // bytes

  private final val IvLength = 16

  private final val DataPosition = SaltLength + IvLength

  def make[F[+_]: Sync](config: CryptoConfig): F[CryptoProvider[F]] =
    for {
      random <- Random.javaSecuritySecureRandom[F]
      encoder = Base64.getEncoder
      decoder = Base64.getDecoder
    } yield new CryptoProviderImpl[F](
      config,
      random,
      encoder,
      decoder,
    )

}