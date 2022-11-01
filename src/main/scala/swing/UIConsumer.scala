package swing

import cats.effect.*
import cats.syntax.all.*
import effects.SummonerK
import fs2.{Compiler, Stream}

trait UIConsumer[F[_]] extends Swing[F]:
  
  extension [T](source: Stream[F, T])
    def forkWithEffect(consumer: T => Unit): Resource[F, Unit]

object UIConsumer extends SummonerK[UIConsumer]:

  given [F[_]: Spawn](using cev: Compiler[F, F], swing: Swing[F]): UIConsumer[F] with
    export swing.*
    
    extension [T](source: Stream[F, T])
      override def forkWithEffect(consumer: T => Unit): Resource[F, Unit] =
        Spawn[F].background {
          source.foreach { x =>
            onUI(consumer(x))
          }.compile.drain
        }.void

  end given

end UIConsumer
