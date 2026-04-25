package optparse_applicative.types

import scalaprops._
import scalaprops.Property.forAllG

object DocSpec extends Scalaprops {
  private[this] implicit val stringDocGen: Gen[Doc] = Gen.alphaNumString.map(Doc.string(_))
  val lineOrEmptyDocGen = Gen.elements(Doc.line, Doc.Empty)
  val appendDocGen = Gen.listOfN(10, Gen.frequency((3, stringDocGen), (1, lineOrEmptyDocGen))).map {
    _.reduce(Doc.append(_, _))
  }
  def equalDocs(w: Int, d1: Doc, d2: Doc): Boolean =
    Doc.prettyRender(w, d1) == Doc.prettyRender(w, d2)

  val `text append is same as concat of strings` = forAllG(Gen.positiveInt, Gen.alphaNumString, Gen.alphaNumString) {
    (w, s1, s2) => equalDocs(w, Doc.string(s1 ++ s2), Doc.append(Doc.string(s1), Doc.string(s2)))
  }

  val `nesting law` = forAllG(Gen.positiveInt, Gen.positiveInt, Gen.positiveInt, Gen[Doc]) { (w, w2, w3, doc) =>
    val List(nest1, nest2, width) = List(w, w2, w3).sorted
    equalDocs(w, Doc.nest(nest1 + nest2, doc), Doc.nest(nest1, Doc.nest(nest2, doc)))
  }

  val `zero nesting is id` = forAllG(Gen.positiveInt, Gen[Doc]) { (w, doc) => equalDocs(w, Doc.nest(0, doc), doc) }

  val `nesting distributes` = forAllG(Gen.positiveInt, Gen.positiveInt, Gen[Doc], Gen[Doc]) { (w, w2, doc, doc2) =>
    val List(nesting, width) = List(w, w2).sorted
    equalDocs(width, Doc.nest(nesting, Doc.append(doc, doc2)), Doc.nest(nesting, doc).append(Doc.nest(nesting, doc2)))
  }

  val `nesting single line is noop` = forAllG(Gen.positiveInt, Gen.positiveInt, Gen.alphaNumString) { (w, w2, s) =>
    val List(nesting, width) = List(w, w2).sorted
    val noNewlines = s.filter(_ != '\n')
    equalDocs(width, Doc.nest(nesting, Doc.string(noNewlines)), Doc.string(noNewlines))
  }
}
