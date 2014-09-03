name := "Open Transit Indicators - GeoTrellis components"

version := "0.1"

scalaVersion := "2.10.3"

// Useful tool for scala web development. use "./sbt ~re-start" and
// it will recompile and run the server after each save
Revolver.settings

libraryDependencies ++= Seq(
  "io.spray" % "spray-routing" % "1.2.0",
  "io.spray" % "spray-can" % "1.2.0",
  "io.spray" % "spray-client" % "1.2.0",
  "io.spray" %% "spray-json" % "1.2.6",
  "io.spray" % "spray-httpx" % "1.2.0",
  "com.typesafe.akka" %% "akka-actor" % "2.2.4",
  "com.azavea.geotrellis" %% "geotrellis-slick" % "0.10.0-SNAPSHOT",
  "com.azavea" %% "gtfs-parser" % "0.1-SNAPSHOT",
  "com.github.nscala-time" %% "nscala-time" % "1.4.0",
  "org.scalatest" %% "scalatest" % "2.1.5" % "test",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "org.scala-lang" % "scala-compiler" % "2.10.3"
)
