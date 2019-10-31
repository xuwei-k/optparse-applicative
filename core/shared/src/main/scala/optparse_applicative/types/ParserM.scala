package optparse_applicative.types

import scalaz.{IList, Monad, NonEmptyList}
import scalaz.std.option.{none, some}
import scalaz.syntax.applicativePlus._

trait ParserM[R] {
  def run[X](f: R => Parser[X]): Parser[X]
}

object ParserM {
  def fromM[A](p: ParserM[A]): Parser[A] =
    p.run(_.pure[Parser])

  def oneM[A](p: Parser[A]): ParserM[A] =
    new ParserM[A] {
      def run[X](f: A => Parser[X]): Parser[X] = BindP(p, f)
    }

  import scalaz.syntax.bind._

  def manyM[A](p: Parser[A]): ParserM[List[A]] =
    oneM(p.map(some) <+> none[A].pure[Parser]).flatMap {
      case None => List.empty[A].pure
      case Some(x) => manyM(p).map(x :: _)
    }

  def someM[A](p: Parser[A]): ParserM[NonEmptyList[A]] =
    ^(oneM(p), manyM(p).map(IList.fromList))(NonEmptyList.nel)

  implicit val parserMMonad: Monad[ParserM] =
    new Monad[ParserM] {
      def bind[A, B](fa: ParserM[A])(f: A => ParserM[B]): ParserM[B] = {
        val g = f
        new ParserM[B] {
          def run[X](f: B => Parser[X]): Parser[X] =
            fa.run(x => g(x).run(f))
        }
      }

      def point[A](a: => A): ParserM[A] =
        new ParserM[A] {
          def run[X](f: A => Parser[X]): Parser[X] = f(a)
        }
    }
}
