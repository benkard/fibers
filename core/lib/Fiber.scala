package eu.mulk.fibers

import monix.eval.Task
import monix.execution.cancelables.MultiAssignmentCancelable
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.{Observable, OverflowStrategy}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.continuations.{reset, shift}
import scala.util.control.NonFatal

/**
  * Fiber-related functionality.
  */
object Fiber extends FiberConversions with FiberTypes {

  sealed trait Effect[-Out] { type Result }

  private[this] object Effect {

    case object Finish extends Effect[Any] {
      override type Result = Nothing
    }

    case class AwaitTask[T](task: Task[T], scheduler: Scheduler)
        extends Effect[Any] {
      override type Result = Try[T]
    }

    case class AwaitFuture[T](future: Future[T],
                              executionContext: ExecutionContext)
        extends Effect[Any] {
      override type Result = Try[T]
    }

    case class Fail(throwable: Throwable) extends Effect[Any] {
      override type Result = Nothing
    }

    case class Emit[-T](returnValue: T) extends Effect[T] {
      override type Result = Unit
    }

    case object GetFiberVar extends Effect[Any] {
      override type Result = Any
    }

    case class PutFiberVar(value: Any) extends Effect[Any] {
      override type Result = Unit
    }

  }

  private[this] final class FiberState {
    var fiberVar: Any = _
  }

  private[this] def perform[Out](
      effect: Effect[Out]): effect.Result @fiber[Out] = {
    val result = shift { (k: Any ⇒ Any) ⇒
      (effect, k)
    }
    result.asInstanceOf[effect.Result]
  }

  /**
    * Runs and awaits the completion of a [[monix.eval.Task]].
    *
    * Returns either a [[scala.util.Success]] or [[scala.util.Failure]] depending on
    * the result of the [[scala.concurrent.Future]].  If you prefer raising an exception
    * instead, consider destructuring the return value:
    *
    *     val Success(x) = await(...)
    *
    * The supplied [[Scheduler]] is used to run the continuation of the fiber.
    */
  def await[T, Out](task: Task[T])(
      implicit scheduler: Scheduler): Try[T] @fiber[Out] =
    perform(Effect.AwaitTask(task, scheduler))

  /**
    * Awaits the completion of a [[scala.concurrent.Future]].
    *
    * Returns either a [[scala.util.Success]] or [[scala.util.Failure]] depending on
    * the result of the [[scala.concurrent.Future]].  If you prefer raising an exception
    * instead, consider destructuring the return value:
    *
    *     val Success(x) = await(...)
    */
  def await[T, Out](fut: Future[T])(
      implicit ec: ExecutionContext): Try[T] @fiber[Out] =
    perform(Effect.AwaitFuture(fut, ec))

  /**
    * Emits a value to the output [[monix.reactive.Observable]].
    */
  def emit[Out](x: Out): Unit @fiber[Out] =
    perform(Effect.Emit(x))

  /**
    * Replaces the current fiber-local state variable.
    *
    * This can be used like a thread-local variable, but for fibers.
    *
    * @see [[getFiberVar]]
    */
  def putFiberVar[Out](x: Any): Unit @fiber[Out] =
    perform(Effect.PutFiberVar(x))

  /**
    * Gets the current fiber-local state variable.
    *
    * The fiber-local state must have been set with [[putFiberVar]] before.
    *
    * @see [[putFiberVar]]
    */
  def getFiberVar[Out]: Any @fiber[Out] =
    perform(Effect.GetFiberVar)

  /**
    * Runs a fiber.
    *
    * @tparam Out the type of objects [[Fiber.emit]]ted by the fiber.
    * @return     a [[monix.reactive.Observable]] producing the objects
    *             [[Fiber.emit]]ted by the fiber.
    */
  def run[Out](thunk: ⇒ Unit @fiber[Out]): Observable[Out] = {
    var state = new FiberState

    monix.reactive.Observable.create[Out](OverflowStrategy.Unbounded) { out ⇒
      val cancelable = MultiAssignmentCancelable()

      def handle(init: (Effect[Out], Any ⇒ Any)): Unit = {
        var more = init
        var done = false
        cancelable := Cancelable { () ⇒
          if (!done) {
            done = true
            out.onComplete()
          }
        }
        while (!done) {
          try {
            val (effect, continue) = more
            effect match {
              case Effect.AwaitTask(task, scheduler) ⇒
                task.asyncBoundary.runOnComplete({ result ⇒
                  val k = continue(result)
                  handle(k.asInstanceOf[(Effect[Out], Any ⇒ Any)])
                })(scheduler)
                done = true
              case Effect.AwaitFuture(task, executionContext) ⇒
                task.onComplete({ result ⇒
                  val k = continue(result)
                  handle(k.asInstanceOf[(Effect[Out], Any ⇒ Any)])
                })(executionContext)
                done = true
              case Effect.GetFiberVar ⇒
                more = continue(state.fiberVar)
                  .asInstanceOf[(Effect[Out], Any ⇒ Any)]
              case Effect.PutFiberVar(value) ⇒
                state.fiberVar = value
                more = continue(()).asInstanceOf[(Effect[Out], Any ⇒ Any)]
              case Effect.Emit(returnValue) ⇒
                out.onNext(returnValue.asInstanceOf[Out])
                more = continue(()).asInstanceOf[(Effect[Out], Any ⇒ Any)]
              case Effect.Fail(throwable) ⇒
                out.onError(throwable)
                done = true
              case Effect.Finish ⇒
                out.onComplete()
                done = true
            }
          } catch {
            case NonFatal(err) ⇒
              out.onError(err)
              done = true
          }
        }
      }

      try {
        val (effect, continue): (Effect[Out], Any ⇒ Any) = reset {
          thunk
          (Effect.Finish, (_: Any) ⇒ ???)
        }
        handle(effect, continue)
      } catch {
        case NonFatal(err) ⇒
          out.onError(err)
      }

      cancelable
    }
  }

}
