package eu.mulk.fibers

import scala.util.continuations.cpsParam

private[fibers] trait FiberTypes {
  type Effect[_]

  /**
    * Annotates a computation as a fiber.
    *
    * Annotate the return type of a fibrational function with this type
    * annotator.
    *
    * @tparam Out the type of objects [[Fiber.emit]]ted by the fiber.
    * @see [[Fiber.run]]
    */
  type fiber[Out] = cpsParam[Any, (Effect[Out], Any ⇒ Any)]
}
