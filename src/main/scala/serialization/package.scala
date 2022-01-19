import sttp.model.Uri
import upickle.AttributeTagged

package object serialization extends AttributeTagged {

  @inline private final def switchCase(origin: String)(buildTarget: (StringBuilder, Char) => Unit): String = {
    val target = new StringBuilder
    var i = 0
    while(i < origin.length) {
      val char = origin(i)
      buildTarget(target, char)
      i += 1
    }
    target.result()
  }

  private final def camelToSnake(camel: String): String =
    switchCase(camel) { (snake, char) =>
      if(char.isUpper) {
        snake += ' '
        snake += char.toLower
      } else {
        snake += char
      }
    }

  private final def snakeToCamel(snake: String): String = {
    var upper = false
    switchCase(snake) { (camel, char) =>
      if(char == '_') {
        upper = true
      } else if(upper) {
        camel += char.toUpper
        upper = false
      } else {
        camel += char
      }
    }
  }

  override def objectAttributeKeyReadMap(snake: CharSequence): CharSequence =
    snakeToCamel(snake.toString)
  override def objectAttributeKeyWriteMap(camel: CharSequence): CharSequence =
    camelToSnake(camel.toString)

  override def objectTypeKeyReadMap(snake: CharSequence): CharSequence =
    snakeToCamel(snake.toString)
  override def objectTypeKeyWriteMap(camel: CharSequence): CharSequence =
    camelToSnake(camel.toString)

  override implicit def OptionWriter[T: Writer]: Writer[Option[T]] =
    implicitly[Writer[T]].comap[Option[T]] {
      case None => null.asInstanceOf[T]
      case Some(x) => x
    }

  override implicit def OptionReader[T: Reader]: Reader[Option[T]] = {
    new Reader.Delegate[Any, Option[T]](implicitly[Reader[T]].map(Some(_))){
      override def visitNull(index: Int) = None
    }
  }

  implicit val UriReader: Reader[Uri] =
    implicitly[Reader[String]].map(Uri.unsafeParse)

}