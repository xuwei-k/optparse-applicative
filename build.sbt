import sbtrelease._
import ReleaseStateTransformations._

val Scala211 = "2.11.12"
val Scala212 = "2.12.13"
val Scala213 = "2.13.5"
val Scala3 = "3.0.0-RC3"

def gitHash(): String = sys.process.Process("git rev-parse HEAD").lineStream_!.head

val tagName = Def.setting {
  s"v${if (releaseUseGlobalVersion.value) (ThisBuild / version).value
  else version.value}"
}
val tagOrHash = Def.setting {
  if (isSnapshot.value) gitHash() else tagName.value
}

val runAll = TaskKey[Unit]("runAll")

def runAllIn(config: Configuration): Setting[Task[Unit]] = {
  (config / runAll) := {
    val classes = (config / discoveredMainClasses).value
    val runner0 = (run / runner).value
    val cp = (config / fullClasspath).value
    val s = streams.value
    classes.foreach(c => runner0.run(c, Attributed.data(cp), Seq(), s.log))
  }
}

val commonSettings = Def.settings(
  scalapropsCoreSettings,
  scalapropsVersion := "0.8.2",
  organization := "com.github.xuwei-k",
  description := "optparse-applicative is a Scala library for parsing options on the command line, providing a powerful applicative interface for composing these options",
  homepage := Some(url("https://github.com/xuwei-k/optparse-applicative")),
  licenses := Seq(
    "BSD-3-Clause" -> url(s"https://raw.githubusercontent.com/xuwei-k/optparse-applicative/${tagOrHash.value}/LICENSE")
  ),
  commands += Command.command("SetDottyNightlyVersion") {
    s"""++ ${dottyLatestNightlyBuild.get}!""" :: _
  },
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
  publishTo := sonatypePublishToBundle.value,
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
        extracted.runAggregated(extracted.get(thisProjectRef) / (Global / PgpKeys.publishSigned), state)
      },
      enableCrossBuild = true
    ),
    releaseStepCommandAndRemaining(s"+ optparseApplicativeNative/publishSigned"),
    releaseStepCommandAndRemaining("sonatypeBundleRelease"),
    setNextVersion,
    commitNextVersion,
    UpdateReadme.updateReadmeProcess,
    pushChanges
  ),
  scalaVersion := Scala211,
  crossScalaVersions := List(Scala211, Scala212, Scala213, Scala3),
  scalacOptions ++= List(
    "-feature",
    "-deprecation",
    "-unchecked",
    "-language:existentials,higherKinds,implicitConversions"
  ),
  scalacOptions ++= {
    if (isDotty.value) {
      Seq(
        "-Ykind-projector"
      )
    } else {
      Seq(
        "-Xlint"
      )
    }
  },
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, v)) if v <= 12 =>
        Seq("-Xfuture", "-Ypartial-unification")
      case _ =>
        Nil
    }
  },
  (Compile / doc / scalacOptions) ++= {
    val tag = tagOrHash.value
    if (isDotty.value) {
      Nil
    } else {
      Seq(
        "-sourcepath",
        (LocalRootProject / baseDirectory).value.getAbsolutePath,
        "-doc-source-url",
        s"https://github.com/xuwei-k/optparse-applicative/tree/${tag}€{FILE_PATH}.scala"
      )
    }
  },
  libraryDependencies ++= List(
    "com.github.scalaprops" %%% "scalaprops" % scalapropsVersion.value % "test",
    "org.scalaz" %%% "scalaz-core" % "7.3.3"
  ).map(_ withDottyCompat scalaVersion.value),
  libraryDependencies ++= {
    if (isDotty.value) {
      Nil
    } else {
      Seq(
        compilerPlugin("org.typelevel" % "kind-projector" % "0.11.3" cross CrossVersion.full)
      )
    }
  }
)

lazy val optparseApplicative = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("core"))
  .settings(
    commonSettings,
    name := UpdateReadme.optparseApplicativeName
  )
  .nativeSettings(
    scalapropsNativeSettings,
    crossScalaVersions := List(Scala211, Scala212, Scala213)
  )
  .jsSettings(
    scalacOptions ++= {
      val a = (LocalRootProject / baseDirectory).value.toURI.toString
      val g = "https://raw.githubusercontent.com/xuwei-k/optparse-applicative/" + tagOrHash.value
      val key = CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, _)) =>
          "-scalajs-mapSourceURI"
        case _ =>
          "-P:scalajs:mapSourceURI"
      }
      Seq(s"${key}:$a->$g/")
    }
  )

val jvm = optparseApplicative.jvm
val js = optparseApplicative.js
val native = optparseApplicative.native

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
