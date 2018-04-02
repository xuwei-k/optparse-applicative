package example

import java.io.File

import optparse_applicative._

import scalaz.NonEmptyList
import scalaz.syntax.applicativePlus._

object ExampleMain {

  case class Opts(verbose: Boolean, name: String, inputs: NonEmptyList[File], output: Option[File])

  val parseOpts: Parser[Opts] =
    ^^^(
      switch(short('v'), long("verbose")),
      strOption(short('n'), long("name")) <|> pure("<default name>"),
      some(strArgument(metavar("FILE"), help("Files to read")).map(new File(_))),
      optional(strOption(short('f'), long("file"), metavar("FILE")).map(new File(_)))
    )(Opts.apply)

  def main(args: Array[String]): Unit = {
    val opts = execParser(args, "ExampleMain", info(parseOpts <*> helper, progDesc("An example program.")))
    println(opts)
  }

}
