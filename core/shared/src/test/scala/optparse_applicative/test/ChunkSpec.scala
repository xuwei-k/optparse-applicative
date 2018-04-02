package optparse_applicative.test

import optparse_applicative.helpdoc.Chunk
import optparse_applicative.internal._
import optparse_applicative.types.Doc
import scalaz.std.list._
import scalaz.std.string._
import scalaz.syntax.applicative._
import scalaprops._
import scalaprops.Property.forAll
import scalaz.Foldable

object ChunkSpec extends Scalaprops {

  private[this] implicit val strGen = Gen.alphaNumString

  implicit def chunkGen[A: Gen]: Gen[Chunk[A]] =
    Gen.from1(Chunk.apply)

  def equalDocs(w: Int, d1: Doc, d2: Doc): Boolean =
    Doc.prettyRender(w, d1) == Doc.prettyRender(w, d2)

  val `fromList 1` = forAll { xs: List[String] =>
    Chunk.fromList(xs).isEmpty == xs.isEmpty
  }

  val `fromList 2` = forAll { xs: List[String] =>
    Chunk.fromList(xs) == Foldable[List].suml(xs.map(_.point[Chunk]))
  }

  val `extract 1` = forAll { s: String =>
    Chunk.extract(s.point[Chunk]) == s
  }

  val `extract 2` = forAll { x: Chunk[String] =>
    Chunk.extract(x.map(_.pure[Chunk])) == x
  }

  val `fromString 1` = Property.forAllG(Gen.positiveInt, Gen[String]) { (w, s) =>
    equalDocs(w, Chunk.extract(Chunk.fromString(s)), Doc.string(s))
  }

  val `fromString 2` = forAll { s: String =>
    Chunk.fromString(s).isEmpty == s.isEmpty
  }

  val `paragraph` = forAll { s: String =>
    Chunk.paragraph(s).isEmpty == words(s).isEmpty
  }
}
