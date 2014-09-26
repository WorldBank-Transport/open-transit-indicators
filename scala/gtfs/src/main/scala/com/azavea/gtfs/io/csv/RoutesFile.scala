package com.azavea.gtfs.io.csv

import com.azavea.gtfs._

object RoutesFile extends GtfsFile[RouteRecord] {
  val fileName = "routes.txt"
  val isRequired = true

  def parse(path: String): Seq[RouteRecord] =
    (for (r <- CsvParser.fromPath(path)) yield {
      RouteRecord(
        r("route_id").get.intern,
        r("route_short_name").get,
        r("route_long_name").get,
        RouteType(r("route_type").get.toInt),
        r("agency_id").map(_.intern),
        r("route_desc"),
        r("route_url"),
        r("route_color"),
        r("route_text_color")
      )
    }).toSeq
}
