package com.azavea.gtfs.io.csv

import com.azavea.gtfs.Stop
import geotrellis.vector.Point
import geotrellis.slick._

object StopsFile extends GtfsFile[Stop] {
  val fileName = "stops.txt"
  val isRequired = true

  def parse(path: String): Seq[Stop] =
    (for (s <- CsvParser.fromPath(path)) yield {
      println("parsing a Stop")
      val lat = s("stop_lat").get.toDouble
      val lng = s("stop_lon").get.toDouble
      Stop(
        s("stop_id").get.intern,
        s("stop_name").get,
        s("stop_desc"),
        Point(lng, lat).withSRID(4326)
      )
    }).toSeq
}
