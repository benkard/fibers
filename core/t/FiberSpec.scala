package eu.mulk.fibers

import minitest._
import minitest.laws.Checkers
import monix.eval.Task
import monix.execution.Ack.{Continue, Stop}
import monix.execution.Scheduler.Implicits.global
import monix.execution.schedulers.TestScheduler
import monix.execution.{Ack, Cancelable, Scheduler}
import monix.reactive.observers.Subscriber
import monix.reactive.{Consumer, Observable, Observer}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object FiberSpec extends SimpleTestSuite with Checkers {
  import Fiber._

  class FakeException extends RuntimeException

  test("sanity") {
    assert(0 != 1)
  }

  testAsync("can produce nothing") {
    val o = run {}
    for (empty ← o.isEmptyL) {
      assert(empty)
    }
  }

  testAsync("can produce a single value") {
    val o = run[Int] {
      emit(100)
    }
    for (l ← o.toListL) {
      assert(l == List(100))
    }
  }

  testAsync("can produce many values") {
    val o = run[Int] {
      for (x ← 1.to(10).cps)
        emit(x)
    }
    for (l ← o.toListL) {
      assertEquals(l, List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
    }
  }

  testAsync("can throw exceptions before emitting values") {
    val o = run[Int] {
      throw new FakeException
    }
    for (t ← o.toListL.failed) {
      assert(t.isInstanceOf[FakeException])
    }
  }

  testAsync("can throw exceptions after emitting values") {
    val o = run[Int] {
      emit(100)
      throw new FakeException
    }
    for (t ← o.toListL.failed) {
      assert(t.isInstanceOf[FakeException])
    }
  }

  testAsync("can await standard futures") {
    val err = new FakeException
    val f1 = Future(1)
    val f2 = Future.failed(err)
    val f3 = Future(3)
    val o = run[Try[Int]] {
      val y1 = await(f1)
      emit(y1)
      val y2 = await(f2)
      emit(y2)
      val y3 = await(f3)
      emit(y3)
    }
    for (t ← o.toListL) {
      assertEquals(t, List(Success(1), Failure(err), Success(3)))
    }
  }

  testAsync("can run Monix tasks") {
    val err = new FakeException
    val f1 = Task.delay(1)
    val f2 = Task.raiseError(err)
    val f3 = Task.delay(3)
    val o = run[Try[Int]] {
      val y1 = await(f1)
      emit(y1)
      val y2 = await(f2)
      emit(y2)
      val y3 = await(f3)
      emit(y3)
    }
    for (t ← o.toListL) {
      assertEquals(t, List(Success(1), Failure(err), Success(3)))
    }
  }

  testAsync("can spawn two fibers, one waiting for the other") {
    val o1: Observable[Int] = run[Int] {
      emit(1)
      emit(2)
      emit(3)
    }
    lazy val o2: Observable[Int] = run[Int] {
      val Success(a1) = await(o1.drop(0).firstL)
      emit(a1)
      val Success(a2) = await(o1.drop(1).firstL)
      emit(a2)
      val Success(a3) = await(o1.drop(2).firstL)
      emit(a3)
    }
    for (t2 ← o2.toListL) {
      assertEquals(t2, List(1, 2, 3))
    }
  }

  testAsync("can spawn two mutually waiting fibers") {
    lazy val o1: Observable[Int] = run[Int] {
      emit(1)
      val Success(b1) = await(o2.firstL)
      emit(b1 + 1)
      val Success(b2) = await(o2.tail.firstL)
      emit(b2 + 1)
    }.cache
    lazy val o2: Observable[Int] = run[Int] {
      val Success(a1) = await(o1.firstL)
      emit(a1 + 1)
      val Success(a2) = await(o1.tail.firstL)
      emit(a2 + 1)
    }.cache
    for (t1 ← o1.toListL;
         t2 ← o2.toListL) {
      assertEquals(t1, List(1, 3, 5))
      assertEquals(t2, List(2, 4))
    }
  }

  testAsync("can store fiber-local state") {
    lazy val o = run {
      putFiberVar(100)
      emit(1)
      val state = getFiberVar.asInstanceOf[Int]
      emit(state)
    }
    for (l ← o.toListL) {
      assertEquals(l, List(1, 100))
    }
  }

  testAsync("can properly maintain state between two alternating fibers") {
    val o1: Observable[Int] = run {
      putFiberVar(2)
      emit(1)
      val a2 = getFiberVar
      putFiberVar(3)
      emit(a2)
      val a3 = getFiberVar
      emit(a3)
    }
    lazy val o2: Observable[Int] = run {
      putFiberVar(100)
      val Success(a1) = await(o1.drop(0).firstL)
      emit(a1)
      emit(getFiberVar)
      val Success(a2) = await(o1.drop(1).firstL)
      emit(a2)
      emit(getFiberVar)
      val Success(a3) = await(o1.drop(2).firstL)
      emit(a3)
      emit(getFiberVar)
    }
    for (t2 ← o2.toListL) {
      assertEquals(t2, List(1, 100, 2, 100, 3, 100))
    }
  }

  testAsync("can be stopped") {
    var f3executed = false

    val f1 = Task.delay(1)
    val f2 = Task.delay(2)
    val f3 = Task.delay({ f3executed = true; 3 })

    val o: Observable[Int] = run {
      val Success(y1) = await(f1)
      emit(y1)
      val Success(y2) = await(f2)
      emit(y2)
      val Success(y3) = await(f3)
      emit(y3)
    }

    val consumeTask = o.consumeWith(Consumer.create[Int, Int] {
      (scheduler, cancelable, cb) ⇒
        new Subscriber.Sync[Int] {
          var triggered = 0

          override implicit def scheduler: Scheduler =
            monix.execution.Scheduler.Implicits.global

          override def onNext(elem: Int): Ack = {
            triggered += 1
            if (triggered >= 2) {
              cb.onSuccess(triggered)
              Stop
            } else {
              Continue
            }
          }

          override def onError(ex: Throwable): Unit =
            cb.onError(ex)

          override def onComplete(): Unit =
            ()
        }
    })

    for (result ← consumeTask) {
      assertEquals(result, 2)
      assert(!f3executed)
    }
  }

  test("can be cancelled") {
    implicit val sched: TestScheduler = TestScheduler()

    var f3executed = false

    val f1 = Task.delay(1)
    val f2 = Task.delay(2)
    val f3 = Task.delay({ f3executed = true; 3 })

    var triggered = 0
    val observer = new Observer[Int] {
      def onNext(elem: Int): Future[Ack] = {
        triggered += 1
        Continue
      }
      def onError(ex: Throwable): Unit = ()
      def onComplete(): Unit = ()
    }

    var subscription: Cancelable = null

    val o: Observable[Int] = run {
      val Success(y1) = await(f1)(sched)
      emit(y1)
      subscription.cancel()
      val Success(y2) = await(f2)(sched)
      emit(y2)
      val Success(y3) = await(f3)(sched)
      emit(y3)
    }

    subscription = o.subscribe(observer)(sched)

    assertEquals(triggered, 0)
    sched.tick()
    assertEquals(triggered, 1)
  }
}
