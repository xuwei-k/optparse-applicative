package object optparse_applicative
    extends builder.Builder
    with common.Common
    with extra.Extra
    with helpdoc.Help
    with types.ParserFunctions {
  type Parser[A] = types.Parser[A]
  type ParserInfo[A] = types.ParserInfo[A]
}
