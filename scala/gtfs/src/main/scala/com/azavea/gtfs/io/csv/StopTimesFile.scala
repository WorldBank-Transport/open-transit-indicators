package com.azavea.gtfs.io.csv

import com.azavea.gtfs._

import scala.collection.mutable

object StopTimesFile extends GtfsFile[StopTimeRecord] {
  val fileName = "stop_times.txt"
  val isRequired = true

  def parse(path: String): Seq[StopTimeRecord] =
    (for (s <- CsvParser.fromPath(path)) yield {
      StopTimeRecord(
        s("stop_id").get.intern,
        s("trip_id").get.intern,
        s("stop_sequence").get.toInt,
        s("arrival_time").get,
        s("departure_time").get,
        s("shape_dist_traveled").flatMap{ geomString =>
          geomString.trim match {
            case "" => None //We can have a column, but no value, sad
            case s => Some(s.toDouble)
          }
        }
      )
    }).toSeq
}
