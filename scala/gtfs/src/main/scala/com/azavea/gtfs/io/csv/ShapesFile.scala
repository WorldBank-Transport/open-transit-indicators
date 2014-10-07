package com.azavea.gtfs.io.csv

import com.azavea.gtfs.TripShape
import geotrellis.vector.{Point, Line}
import geotrellis.slick._

object ShapesFile extends GtfsFile[TripShape] {
  val fileName = "shapes.txt"
  val isRequired = false

  def parse(path: String): Seq[TripShape] =
    (for (f <- CsvParser.fromPath(path)) yield {
      println("parsing a TripShape")
        (
          f("shape_id").get.intern,
          f("shape_pt_lat").get.toDouble,
          f("shape_pt_lon").get.toDouble,
          f("shape_pt_sequence").get.toInt
        )
      })
      .toList
      .groupBy(_._1)
      .map { case (k, t) =>
        val sorted = t.sortBy(_._4)
        val points = sorted.map{r => Point(r._3, r._2)}
        TripShape(k, Line(points).withSRID(4326))
       }
      .toSeq
}
