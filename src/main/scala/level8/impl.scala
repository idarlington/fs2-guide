package level8

import fs2._

object impl {

  /**
    * Maps the supplied stateful function over each element,
    * outputting the final state and the accumulated outputs
    */
  def evalMapAccumulate[F[_], S, O, O2](s1: Stream[F, O])(init: S)(
      f: (S, O) => F[(S, O2)]): Stream[F, (S, O2)] = {

    def go(s: S, in: Stream[F, O]): Pull[F, (S, O2), Unit] =
      in.pull.uncons1.flatMap {
        case Some((element, newStream)) =>
          Pull.eval(f(s, element)).flatMap { o =>
            Pull.output1(o) >> go(o._1, newStream)
          }
        case None =>
          Pull.done
      }

    go(init, s1).stream
  }

  /**
    * Emits only inputs which match the supplied predicate
    */
  def filter[F[_], O](s1: Stream[F, O])(f: O => Boolean): Stream[F, O] = {

    def go(in: Stream[F, O]): Pull[F, O, Unit] = {
      in.pull.uncons1.flatMap {
        case Some((chunkElement, newStream)) =>
          if (f(chunkElement)) {
            Pull.output1(chunkElement) >> go(newStream)
          } else {
            Pull.pure({}) >> go(newStream)
          }

        case None =>
          Pull.done
      }
    }

    go(s1).stream
  }
}
