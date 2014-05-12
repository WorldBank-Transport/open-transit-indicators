name := "Open Transit Indicators - GeoTrellis components"

version := "0.1"

scalaVersion := "2.10.3"

// Useful tool for scala web development. use "./sbt ~re-start" and
// it will recompile and run the server after each save
// Temporarily commented out while the dependencies fail to install
Revolver.settings

libraryDependencies ++= Seq(
  "io.spray" % "spray-routing" % "1.2.0",
  "io.spray" % "spray-can" % "1.2.0",
  // Ensure openjdk-7-jdk is installed via apt (if you provisioned before the PR this dependency
  //    was added, it will not be installed)
  // We temporarily require a development version of geotrellis
  // Use the following steps to get it running:
  //   1) cd ~
  //   2) git clone https://github.com/echeipesh/geotrellis.git
  //   3) cd geotrellis
  //   4) git checkout -b feature/slick origin/feature/slick
  //   5) ./sbt
  //   6) For each project in: 'proj4, 'feature', 'slick', run:
  //        a) project <projname>
  //        b) compile
  //        c) publish-local
  //   7) exit (to exit the sbt prompt, then continue with the gtfs-parser manual
  //            installation below)
  "com.azavea.geotrellis" % "geotrellis-feature_2.10" % "0.10.0-SNAPSHOT",
  "com.azavea.geotrellis" % "geotrellis-proj4_2.10" % "0.10.0-SNAPSHOT",
  "com.azavea.geotrellis" % "geotrellis-slick_2.10" % "0.10.0-SNAPSHOT",
  "com.azavea.geotrellis" % "geotrellis_2.10" % "0.9.1",
  // The following package is still in development, but will be published to Maven shortly.
  // In the meantime, the code must be compiled and published locally in order to work.
  // Use the following steps to do so:
  //   1) git clone https://github.com/echeipesh/gtfs-parser.git
  //   2) cd gtfs-parser
  //   3) git checkout -b feature/slick origin/feature/slick
  //   4) ./sbt
  //   5) publish-local
  // These steps haven't been added to the provisioning script, because they will soon be obsolete.
  // Note: make sure to perform these commands on the same machine/VM where GeoTrellis is running.
  //        The repo will only be available to whatever user ran the publish-local command. If not
  //        run as sudo, the oti-geotrellis service will still fail
  "com.azavea" % "gtfs-parser_2.10" % "0.1-SNAPSHOT",
  "com.github.nscala-time" % "nscala-time_2.10" % "0.8.0",
  "com.typesafe.slick" % "slick_2.10" % "2.0.1",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "postgresql" % "postgresql" % "9.1-901.jdbc4"
)
