import sbtcrossproject.{crossProject, CrossType}

val Scala211 = "2.11.12"
val Scala212 = "2.12.4"

val runAll = TaskKey[Unit]("runAll")

def runAllIn(config: Configuration): Setting[Task[Unit]] = {
  runAll in config := {
    val classes = (discoveredMainClasses in config).value
    val runner0 = (runner in run).value
    val cp = (fullClasspath in config).value
    val s = streams.value
    classes.foreach(c => runner0.run(c, Attributed.data(cp), Seq(), s.log))
  }
}

val commonSettings = Seq[SettingsDefinition](
  scalapropsCoreSettings,
  scalapropsVersion := "0.5.4",
  organization := "com.github.xuwei-k",
  scalaVersion := Scala211,
  crossScalaVersions := List(Scala211, Scala212),
  scalacOptions ++= List(
    "-feature",
    "-deprecation",
    "-unchecked",
    "-Xlint",
    "-language:existentials",
    "-language:higherKinds"
  ),
  libraryDependencies ++= List(
    "com.github.scalaprops" %%% "scalaprops" % scalapropsVersion.value % "test",
    "org.scalaz" %%% "scalaz-core" % "7.2.20"
  ),
  addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.6" cross CrossVersion.binary)
).flatMap(_.settings)

lazy val optparseApplicative = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("core"))
  .settings(
    commonSettings,
    name := "optparse-applicative"
  )
  .nativeSettings(
    scalapropsNativeSettings,
    crossScalaVersions := Seq(Scala211)
  )

val jvm = optparseApplicative.jvm.withId("jvm")
val js = optparseApplicative.js.withId("js")
val native = optparseApplicative.native.withId("native")

val noPublish = Seq(
  publish := {},
  publishLocal := {},
  PgpKeys.publishSigned := {},
  PgpKeys.publishLocalSigned := {},
  publishArtifact := false
)

val example = project
  .in(file("example"))
  .settings(
    commonSettings,
    runAllIn(Compile),
    noPublish
  )
  .dependsOn(
    jvm
  )

lazy val root = project
  .in(file("."))
  .settings(
    noPublish
  )
  .aggregate(
    // ignore native on purpose
    jvm,
    js,
    example
  )
