name := "fibers"
mainClass in Compile := None

inThisBuild(Seq(
  organization := "eu.mulk",

  run / fork := true,
  cancelable := true,

  scalacOptions ++= Seq(
    "-deprecation",
  ),

  licenses += ("AGPL-V3", url("https://www.gnu.org/licenses/agpl-3.0.html"))
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
      "io.monix" %% "monix-reactive" % "2.3.3",
    ),

    // Minitest
    libraryDependencies ++= Seq(
      "io.monix" %% "minitest"      % "2.2.1" % "test",
      "io.monix" %% "minitest-laws" % "2.2.1" % "test",
    ),
    testFrameworks += new TestFramework("minitest.runner.Framework"),
  )
