package effects

trait Summoner[Typeclass[_]] {

  def apply[T: Typeclass]: Typeclass[T] = implicitly[Typeclass[T]]

}