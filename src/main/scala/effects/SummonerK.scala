package effects

trait SummonerK[Effect[_[_]]] {

  def apply[F[_]: Effect]: Effect[F] = implicitly[Effect[F]]

}