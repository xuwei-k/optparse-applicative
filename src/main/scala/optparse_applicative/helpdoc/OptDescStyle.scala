package optparse_applicative.helpdoc

import optparse_applicative.types.Doc

/** Style for rendering an option. */
final case class OptDescStyle(sep: Doc, hidden: Boolean, surround: Boolean)
