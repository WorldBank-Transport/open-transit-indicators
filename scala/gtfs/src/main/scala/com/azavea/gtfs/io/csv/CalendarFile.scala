package com.azavea.gtfs.io.csv

import com.azavea.gtfs._

import scala.collection.mutable

object CalendarFile extends GtfsFile[CalendarRecord] {
  val fileName = "calendar.txt"
  val isRequired = false

  def parse(path: String): Seq[CalendarRecord] =
    (for (c <- CsvParser.fromPath(path)) yield {
      CalendarRecord(
        c("service_id").get.intern,
        c("start_date").get,
        c("end_date").get,
        Array(
          c("monday").get == "1",
          c("tuesday").get == "1",
          c("wednesday").get == "1",
          c("thursday").get == "1",
          c("friday").get == "1",
          c("saturday").get == "1",
          c("sunday").get == "1"
        )
      )
    }).toSeq
}
