import sbt._
import sbt.Keys._

object Build extends Build {

  override lazy val settings =
    super.settings ++ Seq(shellPrompt := { s => Project.extract(s).currentProject.id + " > " })

  lazy val root = Project("OTI_ROOT", file("."))
    .aggregate(opentransit)

  lazy val gtfs =
    Project("gtfs", file("gtfs"))
      .settings(
        name := "gtfs",
        organization := "com.azavea",
        version := "0.1-SNAPSHOT",
        scalaVersion := "2.10.3",
        scalacOptions ++=
          Seq("-deprecation",
            "-unchecked",
            "-Yinline-warnings",
            "-language:implicitConversions",
            "-language:reflectiveCalls",
            "-language:postfixOps",
            "-language:existentials",
            "-feature"),

      libraryDependencies ++=
        Seq(
          "org.apache.servicemix.bundles" % "org.apache.servicemix.bundles.commons-csv" % "1.0-r706900_3",

          "org.scalatest" % "scalatest_2.10" % "2.1.0" % "test",
          "com.github.nscala-time" %% "nscala-time" % "1.4.0",

          "commons-io" % "commons-io" % "2.4",

          "com.azavea.geotrellis" %% "geotrellis-vector" % "0.10.0-SNAPSHOT",
          "com.azavea.geotrellis" %% "geotrellis-proj4" % "0.10.0-SNAPSHOT",
          "com.azavea.geotrellis" %% "geotrellis-slick" % "0.10.0-SNAPSHOT",

          "joda-time" % "joda-time" % "2.5",
          "org.joda" % "joda-convert" % "1.7",
          "com.github.tototoshi" %% "slick-joda-mapper" % "1.2.0"
        )
       )

  lazy val gtfsTest =
    Project("gtfs-test", file("gtfs-test"))
      .settings(
        name := "gtfs-test",
        organization := "com.azavea",
        version := "0.1-SNAPSHOT",
        scalaVersion := "2.10.3",
        scalacOptions ++=
          Seq("-deprecation",
            "-unchecked",
            "-Yinline-warnings",
            "-language:implicitConversions",
            "-language:reflectiveCalls",
            "-language:postfixOps",
            "-language:existentials",
            "-feature"),
        libraryDependencies ++= Seq(
          "org.scalatest" %% "scalatest" % "2.1.5"
        )
       )
      .dependsOn(gtfs, testkit)

  lazy val opentransit =
    Project("opentransit", file("opentransit"))
      .settings(
        name := "gtfs-parser",
        organization := "com.azavea",
        version := "0.1-SNAPSHOT",
        scalaVersion := "2.10.3",
        scalacOptions ++=
          Seq("-deprecation",
            "-unchecked",
            "-Yinline-warnings",
            "-language:implicitConversions",
            "-language:reflectiveCalls",
            "-language:postfixOps",
            "-language:existentials",
            "-feature"),
        libraryDependencies ++= Seq(
          "io.spray" % "spray-routing" % "1.2.0",
          "io.spray" % "spray-can" % "1.2.0",
          "io.spray" % "spray-client" % "1.2.0",
          "io.spray" %% "spray-json" % "1.2.6",
          "io.spray" % "spray-httpx" % "1.2.0",
          "com.typesafe.akka" %% "akka-actor" % "2.2.4",
          "com.github.nscala-time" %% "nscala-time" % "1.4.0",
          "org.scalatest" %% "scalatest" % "2.1.5" % "test",
          "org.scala-lang" % "scala-compiler" % "2.10.3",
          "ch.qos.logback" % "logback-classic" % "1.1.1",
          "org.clapper" %% "grizzled-slf4j" % "1.0.2"
        )
       )
      .settings(spray.revolver.RevolverPlugin.Revolver.settings:_*)
      .dependsOn(gtfs)

  lazy val opentransitTest =
    Project("opentransit-test", file("opentransit-test"))
      .settings(
        name := "opentransit-test",
        organization := "com.azavea",
        fork := true,
        version := "0.1-SNAPSHOT",
        scalaVersion := "2.10.3",
        scalacOptions ++=
          Seq("-deprecation",
            "-unchecked",
            "-Yinline-warnings",
            "-language:implicitConversions",
            "-language:reflectiveCalls",
            "-language:postfixOps",
            "-language:existentials",
            "-feature"),
        libraryDependencies ++= Seq(
          "org.scalatest" %% "scalatest" % "2.1.5",
          "io.spray" % "spray-testkit" % "1.2.0"
        )
       )
      .dependsOn(opentransit, testkit)

  lazy val testkit =
    Project("testkit", file("testkit"))
      .settings(
        name := "opentransit-testkit",
        organization := "com.azavea",
        version := "0.1-SNAPSHOT",
        scalaVersion := "2.10.3",
        libraryDependencies ++= Seq(
          "com.typesafe" % "config" % "1.2.1",
          "com.azavea.geotrellis" %% "geotrellis-slick" % "0.10.0-SNAPSHOT",
          "org.scalatest" %% "scalatest" % "2.1.5"
        )
       )
      .dependsOn(gtfs)
}
