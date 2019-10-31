package optparse_applicative

import optparse_applicative.types.{ParseError, ParserPrefs, ReadM}

import scalaz.\/

package object internal {
  def runP[A](p: P[A], pprefs: ParserPrefs): (Context, ParseError \/ A) =
    p.run.run.run.run(pprefs)

  def uncons[A](xs: List[A]): Option[(A, List[A])] =
    xs match {
      case Nil => None
      case x :: xs => Some((x, xs))
    }

  def min[A](a1: A, a2: A)(implicit A: Ordering[A]): A =
    A.min(a1, a2)

  def words(s: String): List[String] =
    s.split("\\s+").toList.filter(_.nonEmpty)

  def unwords(xs: List[String]): String =
    xs.mkString(" ")

  def runReadM[F[_], A](reader: ReadM[A], s: String)(implicit F: MonadP[F]): F[A] =
    P.hoistEither(reader.run.run(s))
}
