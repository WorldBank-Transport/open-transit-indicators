package com.azavea.gtfs.io.csv

import scala.util.Try

import com.azavea.gtfs._

object TripsFile extends GtfsFile[TripRecord] {
  val fileName = "trips.txt"
  val isRequired = true

  def parse(path: String): Seq[TripRecord] =
    (for (t <- CsvParser.fromPath(path)) yield {
      TripRecord(
        t("trip_id").get.intern,
        t("service_id").get.intern,
        t("route_id").get.intern,
        t("trip_headsign"),
        Try(t("direction_id").get.toInt).toOption,
        t("shape_id").map(_.intern)
      )
    }).toSeq
}
