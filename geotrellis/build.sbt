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
  "io.spray" % "spray-json_2.10" % "1.2.6",
  "io.spray" % "spray-httpx" % "1.2.0",
  "com.azavea.geotrellis" % "geotrellis-vector_2.10" % "0.10.0-SNAPSHOT",
  "com.azavea.geotrellis" % "geotrellis-proj4_2.10" % "0.10.0-SNAPSHOT",
  "com.azavea.geotrellis" % "geotrellis-slick_2.10" % "0.10.0-SNAPSHOT",
  "com.azavea.geotrellis" % "geotrellis_2.10" % "0.9.1",
  "com.azavea" % "gtfs-parser_2.10" % "0.1-SNAPSHOT",
  "com.github.nscala-time" % "nscala-time_2.10" % "0.8.0",
  "com.typesafe.slick" % "slick_2.10" % "2.0.1",
  "org.scalatest" % "scalatest_2.10" % "2.1.5" % "test",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "postgresql" % "postgresql" % "9.1-901.jdbc4"
)
