package eu.mulk.fibers

import scala.collection.generic.CanBuildFrom
import scala.collection.{GenTraversableOnce, IterableLike}
import scala.language.implicitConversions

/**
  * Implicit conversions for convenient use of containers from within
  * fibers.
  */
private[fibers] trait FiberConversions extends FiberTypes {
  //
  // This is based on code by James Earl Dougles, taken from:
  //
  //     https://earldouglas.com/posts/monadic-continuations.html
  //

  /**
    * Defines the `cps` helper on [[IterableLike]] objects, which you can call to
    * traverse them from fibers.
    */
  implicit def cpsIterable[A, Repr](xs: IterableLike[A, Repr]) = new {

    /**
      * Provides fiber-compatible iteration functions.
      */
    def cps = new {
      def foreach[B, T](f: A => Any @fiber[T]): Unit @fiber[T] = {
        val it = xs.iterator
        while (it.hasNext) f(it.next)
      }
      def map[B, That, T](f: A => B @fiber[T])(
          implicit cbf: CanBuildFrom[Repr, B, That]): That @fiber[T] = {
        val b = cbf(xs.repr)
        foreach(b += f(_))
        b.result
      }
      def flatMap[B, That, T](f: A => GenTraversableOnce[B] @fiber[T])(
          implicit cbf: CanBuildFrom[Repr, B, That]): That @fiber[T] = {
        val b = cbf(xs.repr)
        for (x <- this) b ++= f(x)
        b.result
      }
    }
  }
}
