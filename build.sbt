name := "fibers"
mainClass in Compile := None

inThisBuild(Seq(
  version := "0.1.0-SNAPSHOT",
  organization := "eu.mulk",

  run / fork := true,
  cancelable := true,

  scalacOptions ++= Seq(
    "-deprecation",
  ),
))

lazy val core = (project in file("core"))
  .settings(
    name := "fibers-core",

    scalaSource in Compile := baseDirectory.value / "lib",
    scalaSource in Test := baseDirectory.value / "t",

    // Continuations
    addCompilerPlugin("org.scala-lang.plugins" % "scala-continuations-plugin_2.12.2" % "1.0.3"),
    libraryDependencies += "org.scala-lang.plugins" %% "scala-continuations-library" % "1.0.3",
    scalacOptions += "-P:continuations:enable",

    // Monix
    libraryDependencies ++= Seq(
      "io.monix" %% "monix"      % "2.3.0",
      "io.monix" %% "monix-cats" % "2.3.0",
    ),

    // Minitest
    libraryDependencies ++= Seq(
      "io.monix" %% "minitest"      % "2.0.0" % "test",
      "io.monix" %% "minitest-laws" % "2.0.0" % "test",
    ),
    testFrameworks += new TestFramework("minitest.runner.Framework"),
  )
