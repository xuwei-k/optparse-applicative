scalapropsSettings

scalapropsVersion := "0.5.4"

organization := "com.github.xuwei-k"

name := "optparse-applicative"

scalaVersion := "2.11.12"

crossScalaVersions := List("2.10.7", "2.11.12", "2.12.4")

scalacOptions ++= List(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-Xlint",
  "-language:existentials",
  "-language:higherKinds")

libraryDependencies ++= List(
  "com.github.scalaprops" %% "scalaprops-gen" % scalapropsVersion.value,
  "org.scalaz" %% "scalaz-core" % "7.2.20"
)

addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.6" cross CrossVersion.binary)
