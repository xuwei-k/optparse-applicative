package optparse_applicative.builder.internal

import optparse_applicative.types.OptName


trait HasName[F[_]] {

  def name[A](n: OptName, fa: F[A]): F[A]

}

trait HasMetavar[F[_]] {
}
