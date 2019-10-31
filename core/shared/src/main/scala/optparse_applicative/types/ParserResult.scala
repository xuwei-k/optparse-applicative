package optparse_applicative.types

import optparse_applicative.helpdoc.ParserHelp

import scalaz.Functor

sealed trait ParserResult[A]

case class Success[A](a: A) extends ParserResult[A]

case class Failure[A](failure: ParserFailure[ParserHelp]) extends ParserResult[A]

sealed trait ExitCode {
  final def toInt: Int =
    this match {
      case ExitSuccess => 0
      case ExitFailure(i) => i
    }
}
case object ExitSuccess extends ExitCode
case class ExitFailure(code: Int) extends ExitCode

case class ParserFailure[H](run: String => (H, ExitCode, Int))

object ParserFailure {
  implicit val parserFailureFunctor: Functor[ParserFailure] =
    new Functor[ParserFailure] {
      def map[A, B](fa: ParserFailure[A])(f: A => B): ParserFailure[B] =
        ParserFailure { progName =>
          val (h, exit, cols) = fa.run(progName)
          (f(h), exit, cols)
        }
    }
}
