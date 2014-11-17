package com.azavea.gtfs

import geotrellis.vector._
import geotrellis.vector.json._
import geotrellis.vector.io.WKT

import java.io._

import scala.collection.JavaConversions._

object TEMP {
  def write(path: String, txt: String): Unit = {
    import java.nio.file.{Paths, Files}
    import java.nio.charset.StandardCharsets

    Files.write(Paths.get(path), txt.getBytes(StandardCharsets.UTF_8))
  }

  def read(path: String): String =
    scala.io.Source.fromFile(path).getLines.mkString

  def main(args: Array[String]): Unit = 
//    runProjected
    runAll

  def runProjected = {
    val path = "/Users/rob/proj/wb/data/gtfs-geojson/septaRail.geojson"

    val lines = read(path).parseGeoJson[JsonFeatureCollection].getAllLines
    Timer.timedTask("  Unioning Septa projected lines") {
      MultiLine(lines).union match {
        case MultiLineResult(ml) => ml
        case LineResult(l) => MultiLine(l)
        case NoResult => MultiLine()
      }
    }
  }

  def runAll = {
    val topLevel = "/Users/rob/proj/wb/data/gtfs"

    val outDir = "/Users/rob/proj/wb/data/gtfs-geojson"

    val subDirs = new File(topLevel).listFiles.filter { f => f.isDirectory }

    for(d <- subDirs) {
      val name = d.getName

      if(name == "WASHINGTON-DC") {
        println(s"Checking $name...")

        val records = GtfsRecords.fromFiles(d.getAbsolutePath)
        val lines =
          records.tripShapes.map(_.line.geom)

        println(s"  Writing WKT...")
        // val txt = lines.map { line => geotrellis.vector.io.WKT.write(line) }.mkString("\n")
        // write(s"$outDir/${name}.wkt", txt)

        // val wkt = lines.map { line => WKT.write(line) }
        // val lines2 = wkt.map { line => WKT.read[Line](line) }

        val ml = MultiLine(lines)
        write(s"$outDir/${name}.wkt", WKT.write(ml))

        val unioned = ml.jtsGeom.union
        import com.vividsolutions.jts.io.{WKTReader, WKTWriter}
        val writer = new WKTWriter()
        write(s"$outDir/${name}-unioned.wkt", writer.write(unioned))

        println(s"Valid? ${unioned.isValid}")



        // println(s"  Writing features...")
        // write(s"$outDir/${name}.json", lines.toGeoJson)

        // println(s"  Creating Multilines...")
        // val ml = 
        //   Timer.timedTask("  MultiLine created") {
        //     MultiLine(lines)
        //   }

        // println(s"  Starting to union lines...")
        // val unioned =
        //   Timer.timedTask("  Unioning lines") {
        //     ml.union match {
        //       case MultiLineResult(ml) => ml
        //       case LineResult(l) => MultiLine(l)
        //       case NoResult => MultiLine()
        //     }
        //   }
      }
    }
  }
}
