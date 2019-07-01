package level6

import fs2.{Chunk, Pull, Stream}

object impl {

  /**
    * Emits the longest prefix for which all elements test true according to predicates
    */
  def takeWhile[F[_], O](s: Stream[F, O])(p: O => Boolean): Stream[F, O] = {
    def go(s: Stream[F, O]): Pull[F, O, Unit] = {
      s.pull.uncons1.flatMap[F, O, Unit] {
        case Some((chunkElement, newStream)) =>
          if (p(chunkElement)) {
            Pull.output(Chunk.singleton(chunkElement)) >> go(newStream)
          } else {
            Pull.done
          }

        case None => Pull.done
      }
    }

    go(s).stream
  }

  /**
    * Emits the separator between every pair of elements in the stream
    */
  def intersperse[F[_], O](s: Stream[F, O])(separator: O): Stream[F, O] = {
    def go(s: Stream[F, O]): Pull[F, O, Unit] =
      s.pull.uncons.flatMap[F, O, Unit] {
        case Some((chunk, stream)) =>
          Pull.output {
            chunk.flatMap[O](element => Chunk.seq[O](Seq(separator, element)))
          } >> go(stream)

        case None => Pull.done
      }

    go(s).stream.drop(1)
  }

  /**
    * Left fold which outputs all intermediate results
    */
  def scan[F[_], O, O2](s: Stream[F, O])(z: O2)(
      f: (O2, O) => O2): Stream[F, O2] = {

    def go(stream: Stream[F, O])(acc: O2): Pull[F, O2, Unit] = {
      stream.pull.uncons1.flatMap[F, O2, Unit] {
        case Some((element, newStream)) =>
          val result = f(acc, element)
          Pull.output { Chunk(acc) } >> go(newStream)(result)
        case None =>
          Pull.output { Chunk(acc) } >> Pull.done
      }
    }

    def otherSolution(stream: Stream[F, O])(acc: O2): Pull[F, O2, Unit] = {
      stream.pull.uncons1.flatMap[F, O2, Unit] {
        case Some((element, newStream)) =>
          val result = f(acc, element)
          Pull.output { Chunk(result) } >> otherSolution(newStream)(result)
        case None =>
          Pull.done
      }
    }

    go(s)(z).stream /* or Stream(z) ++ otherSolution(s)(z).stream */
  }

}
