import sbtrelease._
import ReleaseStateTransformations._
import sbtcrossproject.{crossProject, CrossType}

val Scala211 = "2.11.12"
val Scala212 = "2.12.6"
val Scala213 = "2.13.0-M4"

def gitHash(): String = sys.process.Process("git rev-parse HEAD").lineStream_!.head

val tagName = Def.setting {
  s"v${if (releaseUseGlobalVersion.value) (version in ThisBuild).value
  else version.value}"
}
val tagOrHash = Def.setting {
  if (isSnapshot.value) gitHash() else tagName.value
}

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
  scalapropsVersion := "0.5.5",
  organization := "com.github.xuwei-k",
  description := "optparse-applicative is a Scala library for parsing options on the command line, providing a powerful applicative interface for composing these options",
  homepage := Some(url("https://github.com/xuwei-k/optparse-applicative")),
  licenses := Seq(
    "BSD-3-Clause" -> url(s"https://raw.githubusercontent.com/xuwei-k/optparse-applicative/${tagOrHash.value}/LICENSE")
  ),
  pomExtra := {
    <developers>
      <developer>
        <id>xuwei-k</id>
        <name>Kenji Yoshida</name>
        <url>https://github.com/xuwei-k</url>
      </developer>
    </developers>
    <scm>
      <url>git@github.com:xuwei-k/optparse-applicative.git</url>
      <connection>scm:git:git@github.com:xuwei-k/optparse-applicative.git</connection>
      <tag>{tagOrHash.value}</tag>
    </scm>
  },
  publishTo := sonatypePublishTo.value,
  releaseTagName := tagName.value,
  releaseCrossBuild := true,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    UpdateReadme.updateReadmeProcess,
    tagRelease,
    ReleaseStep(
      action = { state =>
        val extracted = Project extract state
        extracted.runAggregated(PgpKeys.publishSigned in Global in extracted.get(thisProjectRef), state)
      },
      enableCrossBuild = true
    ),
    releaseStepCommandAndRemaining(s"; ++ ${Scala211} ; native/publishSigned"),
    setNextVersion,
    commitNextVersion,
    releaseStepCommand("sonatypeReleaseAll"),
    UpdateReadme.updateReadmeProcess,
    pushChanges
  ),
  scalaVersion := Scala211,
  crossScalaVersions := List(Scala211, Scala212, Scala213),
  scalacOptions ++= List(
    "-feature",
    "-Xfuture",
    "-deprecation",
    "-unchecked",
    "-Xlint",
    "-language:existentials",
    "-language:higherKinds"
  ),
  scalacOptions in (Compile, doc) ++= {
    val tag = tagOrHash.value
    Seq(
      "-sourcepath",
      (baseDirectory in LocalRootProject).value.getAbsolutePath,
      "-doc-source-url",
      s"https://github.com/xuwei-k/optparse-applicative/tree/${tag}€{FILE_PATH}.scala"
    )
  },
  libraryDependencies ++= List(
    "com.github.scalaprops" %%% "scalaprops" % scalapropsVersion.value % "test",
    "org.scalaz" %%% "scalaz-core" % "7.2.23"
  ),
  addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.6" cross CrossVersion.binary)
).flatMap(_.settings)

lazy val optparseApplicative = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("core"))
  .settings(
    commonSettings,
    name := UpdateReadme.optparseApplicativeName
  )
  .nativeSettings(
    scalapropsNativeSettings,
    crossScalaVersions := Seq(Scala211)
  )
  .jsSettings(
    scalacOptions += {
      val a = (baseDirectory in LocalRootProject).value.toURI.toString
      val g = "https://raw.githubusercontent.com/xuwei-k/optparse-applicative/" + tagOrHash.value
      s"-P:scalajs:mapSourceURI:$a->$g/"
    }
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
    commonSettings,
    noPublish
  )
  .aggregate(
    // ignore native on purpose
    jvm,
    js,
    example
  )
