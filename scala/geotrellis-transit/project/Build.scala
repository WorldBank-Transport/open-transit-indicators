import sbt._
import sbt.Keys._

object GeotrellisTransitBuild extends Build {
  val scalaOptions = Seq(
        "-deprecation",
        "-unchecked",
        "-Yclosure-elim",
        "-Yinline-warnings",
        "-optimize",
        "-language:implicitConversions",
        "-language:postfixOps",
        "-language:existentials",
        "-feature"
  )

  val key = AttributeKey[Boolean]("javaOptionsPatched")

  val updateJettyTask = TaskKey[Unit]("update-jetty")

  val updateJettySettings = Seq(
    updateJettyTask <<= (copyResources in Compile, compile in Compile) map {
      (c, p) =>
    }
  )


  lazy val root = 
    Project("root", file(".")).settings(
      organization := "com.azavea.geotrellis",
      name := "geotrellis-transit",
      version := "0.1.0-SNAPSHOT",
      scalaVersion := "2.10.3",
     
      updateJettyTask <<= (copyResources in Compile, compile in Compile) map {
        (c, p) =>
      },
 
      scalacOptions ++= scalaOptions,
      scalacOptions in Compile in doc ++= Seq("-diagrams", "-implicits"),
      parallelExecution := false,

      fork in run := true,

      mainClass := Some("geotrellis.transit.Main"),

      javaOptions in (Compile,run) ++= Seq("-Xmx10G"),

      libraryDependencies ++= Seq(
        "com.azavea.geotrellis" %% "geotrellis-engine" % "0.10.0-SNAPSHOT",
        "com.azavea.geotrellis" %% "geotrellis-jetty" % "0.10.0-SNAPSHOT",
        "com.azavea"            %% "gtfs-parser" % "0.1-SNAPSHOT",
        "io.spray"        % "spray-client"  % "1.2.1",
        "io.spray"        % "spray-routing" % "1.2.1",
        "io.spray"        % "spray-httpx"   % "1.2.1",
        "com.typesafe" % "config" % "1.0.2",
        "org.spire-math" %% "spire" % "0.3.0",
        "org.scalatest" %% "scalatest" % "2.0.M5b" % "test",
        "com.google.guava" % "guava" % "14.0.1"
      ),

      resolvers ++= Seq(
        "Geotools" at "http://download.osgeo.org/webdav/geotools/",
        "opengeo" at "http://repo.opengeo.org/",
        "spray repo" at "http://repo.spray.io/",
        Resolver.sonatypeRepo("snapshots")
      ),

      licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.html")),
      homepage := Some(url("http://github.com/geotrellis/geotrellis-transit")),

      pomExtra := (
        <scm>
          <url>git@github.com:azavea/GeotrellisTransit.git</url>
          <connection>scm:git:git@github.com:geotrellis/geotrellis-transit.git</connection>
          </scm>
          <developers>
          <developer>
          <id>lossyrob</id>
          <name>Rob Emanuele</name>
          <url>http://github.com/lossyrob/</url>
            </developer>
          <developer>
          <id>joshmarcus</id>
          <name>Josh Marcus</name>
          <url>http://github.com/joshmarcus/</url>
            </developer>
          </developers>
      )
    )
}
