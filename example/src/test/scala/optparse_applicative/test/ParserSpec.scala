package optparse_applicative.test

import optparse_applicative._
import example.Commands
import optparse_applicative.types.{Failure, ParserInfo, ParserResult, Success}

import scalaprops._
import scalaprops.Property.forAll
import scalaz.syntax.apply._

import example._

class ParserSpec extends Scalaprops {
  def run[A](pinfo: ParserInfo[A], args: List[String]): ParserResult[A] =
    execParserPure(prefs(), pinfo, args)

  val `dash-dash args` = forAll {
    val result = run(Commands.opts, List("hello", "foo", "--", "--bar", "--", "baz"))
    result match {
      case Success(Hello(List("foo", "--bar", "--", "baz"))) => true
      case _ => false
    }
  }

  val flags: Parser[Int] =
    flag_(1, long("foo")) <|> flag_(2, long("bar")) <|> flag_(3, long("baz"))

  val `test disambiguate` = forAll {
    val result = execParserPure(prefs(disambiguate), info(flags), List("--f"))
    result == Success(1)
  }

  val ambiguous = forAll {
    val result = execParserPure(prefs(disambiguate), info(flags), List("--ba"))
    result match {
      case Success(_) => false
      case Failure(_) => true
    }
  }

  val backtracking = forAll {
    val p2 = switch(short('a'))
    val p1 = ^(subparser(command("c", info(p2))), switch(short('b')))((_, _))
    val i = info(p1 <*> helper)
    val result = execParserPure(prefs(noBacktrack), i, List("c", "-b"))
    result match {
      case Success(_) => false
      case Failure(_) => true
    }
  }
}
