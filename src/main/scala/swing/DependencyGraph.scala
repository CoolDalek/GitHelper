package swing

import cats.*
import cats.effect.*
import cats.syntax.all.*
import effects.SummonerK

import scala.annotation.tailrec
import scala.reflect.*

trait DependencyGraph[F[_]]:

  def delay[Value](pure: Value): Dependency[F, Any, Nothing, Value]

  def require[Depends: ClassTag]: Dependency[F, Depends, Nothing, Depends]

  def provide[Provide: ClassTag](provider: F[Provide]): Dependency[F, Nothing, Provide, Provide]

  def resolve[Provide, Value, Depends <: Provide](graph: Dependency[F, Depends, Provide, Value]): Resource[F, Value]

object DependencyGraph extends SummonerK[DependencyGraph]:
  trait Delayed[F[_], T]:
    def get: F[T]

  object Delayed:
    def const[F[_]: Applicative, T](value: T): Delayed[F, T] = new:
      def get: F[T] = value.pure[F]
  
  opaque type Dependency[F[_], Depends, Provide, Value] = Delayed[F, Value]

  extension [F[_]: DependencyGraph, D, P, V](self: Dependency[F, D, P, V])

    inline def edge[D0, P0, V0](inject: Delayed[F, V] => Dependency[F, D0, P0, V0]): Dependency[F, D & D0, P & P0, V0] =
      inject(self)

    inline def flatMap[D0, P0, V0](inject: Delayed[F, V] => Dependency[F, D0, P0, V0]): Dependency[F, D & D0, P & P0, V0] =
      inject(self)

    inline def map[V0](transform: Delayed[F, V] => V0): Dependency[F, D, P, V0] =
      DependencyGraph[F].delay(transform(self))

    inline def resolve(using D <:< P): Resource[F, V] =
      DependencyGraph[F].resolve(self)

  end extension

  def make[F[_]: Async]: DependencyGraph[F] = new:
    import scala.collection.concurrent.TrieMap
    import java.util.concurrent.atomic.AtomicReference

    class Node[T] extends Delayed[F, T]:
      enum State:
        case Waiting(blocked: Vector[F[T] => Unit])
        case Done(value: F[T])
      import State.*
      private val state = AtomicReference[State](Waiting(Vector.empty))

      def set(value: F[T]): Unit = // ok to fail
        state.get() match
          case wait @ Waiting(blocked) =>
            val done = Done(value)
            if(state.compareAndSet(wait, done))
              blocked.foreach(_.apply(value))
          case _ => ()
      end set

      def get: F[T] = Async[F].defer {
        state.get() match {
          case Done(value) => value
          case _ =>
            Async[F].async_[F[T]] { cb =>
              val cont = (result: F[T]) => cb(Right(result))

              @tailrec
              def loop(): F[Unit] =
                state.get() match
                  case current @ Waiting(blocked) =>
                    val updated = Waiting(blocked.appended(cont))
                    if(state.compareAndSet(current, updated))
                      Async[F].unit
                    else loop()
                  case Done(value) =>
                    Async[F].delay(cont(value))
              end loop

              loop()
            }.flatten
        }
      }
    end Node

    private val dependencies = TrieMap.empty[Class[?], Node[?]]

    def delay[Value](pure: Value): Dependency[F, Any, Nothing, Value] = Delayed.const[F, Value](pure)

    def require[Depends: ClassTag]: Dependency[F, Depends, Nothing, Depends] =
      dependencies.getOrElseUpdate(
        classTag[Depends].runtimeClass,
        Node[Depends],
      ).asInstanceOf[Dependency[F, Depends, Nothing, Depends]]

    def provide[Provide: ClassTag](provider: F[Provide]): Dependency[F, Nothing, Provide, Provide] =
      val node = dependencies.getOrElseUpdate(
        classTag[Provide].runtimeClass,
        Node[Provide],
      ).asInstanceOf[Node[Provide]]
      node.set(provider)
      node
    end provide

    def resolve[Provide, Value, Depends <: Provide](graph: Dependency[F, Depends, Provide, Value]): Resource[F, Value] =
      Resource.eval(graph.get)
    
  end make

export DependencyGraph.{Dependency, Delayed}
