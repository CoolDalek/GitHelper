package effects

trait SummonerK[Effect[_[_]]]:
  inline def apply[F[_]: Effect]: Effect[F] = summon[Effect[F]]
