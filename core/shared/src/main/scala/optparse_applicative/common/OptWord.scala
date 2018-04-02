package optparse_applicative.common

import optparse_applicative.types.OptName

final case class OptWord(name: OptName, value: Option[String])
