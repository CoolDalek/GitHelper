package serialization

import sttp.model.Uri
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import com.github.plokhotnyuk.jsoniter_scala.core.*

import scala.util.control.NonFatal

inline def config = CodecMakerConfig.withFieldNameMapper(JsonCodecMaker.enforce_snake_case)

inline def deriveCodec[T]: JsonValueCodec[T] = JsonCodecMaker.make[T](config)

given JsonValueCodec[Uri] with
  override def decodeValue(in: JsonReader, default: Uri): Uri =
    in.readString(null.asInstanceOf[String]) match
      case null => in.decodeError("Cannot read Uri - no value provided.")
      case value =>
        try Uri.unsafeParse(value)
        catch case NonFatal(exc) => in.decodeError(exc.getMessage)
  end decodeValue

  override def encodeValue(x: Uri, out: JsonWriter): Unit =
    out.writeVal(x.toString)

  override def nullValue: Uri = null.asInstanceOf[Uri] //no need in null value, so noop
end given

inline def read[T: Codec](in: String): T = readFromString[T](in)

inline given[T: JsonValueCodec]: JsonValueCodec[Seq[T]] = deriveCodec[Seq[T]]

type Codec[T] = JsonValueCodec[T]