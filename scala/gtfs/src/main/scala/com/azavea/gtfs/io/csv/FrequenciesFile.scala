package com.azavea.gtfs.io.csv

import com.azavea.gtfs.FrequencyRecord
import com.github.nscala_time.time.Imports._

object FrequenciesFile extends GtfsFile[FrequencyRecord] {
  val fileName = "frequencies.txt"
  val isRequired = false

  def parse(path: String): Seq[FrequencyRecord] =
    (for (f <- CsvParser.fromPath(path)) yield {
      FrequencyRecord(
        f("trip_id").get.intern,
        f("start_time").get,
        f("end_time").get,
        f("headway_secs").get.toInt.seconds
      )
    }).toSeq

}
