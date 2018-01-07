# Fibers for Scala

This library implements **fibers** (or **coroutines**) on top of
delimited continuations as provided by the [Scala CPS compiler
plugin][continuations].

## Usage

Since the library is not currently published in a publically
accessible Maven repository, you have to compile it and publish it
into your local Maven repository yourself:

```bash
sbt publishLocal
```

And then, in your `build.sbt` file:

```scala
// Enable CPS plugin
addCompilerPlugin("org.scala-lang.plugins" % "scala-continuations-plugin_2.12.2" % "1.0.3"),
libraryDependencies += "org.scala-lang.plugins" %% "scala-continuations-library" % "1.0.3",
scalacOptions += "-P:continuations:enable",

// Depend on fibers-core
libraryDependencies ++= Seq(
  "eu.mulk" %% "fibers-core" % "0.1.0-SNAPSHOT",
)
```

## Examples

Awaiting futures and tasks and emitting values:

```scala
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable

import eu.mulk.fibers.Fiber._

import scala.util.Success

val slowBackgroundTask = Task.delay(100)

def produceNumbers: Unit @fiber[Int] = {
  val Success(x) = await(slowBackgroundTask)
  emit(x)
  emit(x*2)
  emit(x*3)
}

val observable: Observable[Int] = run(produceNumbers)
observable.foreachL(println).runAsync  // => 100, 200, 300
```

Using fiber-local state:

```scala
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable

import eu.mulk.fibers.Fiber._

def emitFiberVar: Unit @fiber[Int] = {
  emit(getFiberVar)
}

def produceNumbers: Unit @fiber[Int] = {
  putFiberVar[Int](100)
  emitFiberVar
}

val observable: Observable[Int] = run(produceNumbers)
observable.foreachL(println).runAsync  // => 100
```

For more examples, see the [unit tests][].

[continuations]: https://github.com/scala/scala-continuations
[unit tests]:    core/t/FiberSpec.scala
