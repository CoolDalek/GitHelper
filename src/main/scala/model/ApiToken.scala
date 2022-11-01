package model

import cats.Show
import doobie.util.*
import effects.{PrepareDecryption, PrepareEncryption}
import pureconfig.ConfigReader

object ApiToken:
  opaque type ApiToken = String

  inline given ConfigReader[ApiToken] = TokenAux.stringReader

  inline given Show[ApiToken] = TokenAux.stringShow

  inline given Get[ApiToken] = TokenAux.stringGet

  inline given Put[ApiToken] = TokenAux.stringPut

  given PrepareEncryption[ApiToken] with
    override def charset: ApiToken = PrepareEncryption.Utf8

    override def asString(obj: ApiToken): String = obj

    override def asBytes(obj: ApiToken): Array[Byte] = obj.getBytes(PrepareEncryption.Utf8)
  end given

  given PrepareDecryption[ApiToken] with
    override def charset: ApiToken = PrepareDecryption.Utf8
    
    override def fromString(decodedString: String): ApiToken = decodedString
    
    override def fromBytes(decodedBytes: Array[Byte]): ApiToken = new String(decodedBytes, PrepareDecryption.Utf8)
  end given

export ApiToken.ApiToken

private[model] object TokenAux:

  inline def stringReader: ConfigReader[String] = ConfigReader[String]
  inline def stringShow: Show[String] = Show[String]
  inline def stringGet: Get[String] = Get[String]
  inline def stringPut: Put[String] = Put[String]

end TokenAux