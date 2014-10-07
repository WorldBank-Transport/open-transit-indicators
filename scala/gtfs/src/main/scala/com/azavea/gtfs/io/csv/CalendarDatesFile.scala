package com.azavea.gtfs.io.csv

import com.azavea.gtfs._

import scala.collection.mutable

object CalendarDatesFile extends GtfsFile[CalendarDateRecord] {
  val fileName = "calendar_dates.txt"
  val isRequired = false

  def parse(path: String): Seq[CalendarDateRecord] =
    (for(c <- CsvParser.fromPath(path)) yield {
      println("parsing a CalendarDateRecord")
      val serviceId = c("service_id").get.intern
      CalendarDateRecord(
        serviceId,
        c("date").get,
        c("exception_type").get.toInt match {
          case 1 => true
          case 2 => false
          case x => sys.error(s"Unsupported exception_type($x) for service ${serviceId}")
        }
      )
    }).toSeq
}
