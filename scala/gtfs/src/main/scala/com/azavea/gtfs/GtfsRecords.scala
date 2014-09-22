package com.azavea.gtfs

import scala.slick.jdbc.JdbcBackend.Session

trait GtfsRecords {
/* From GTFS Spec (Revised June 20, 2012)
 *  agency.txt           (IMPLEMENTED)
 *  stops.txt            (IMPLEMENTED)
 *  routes.txt           (IMPLEMENTED)
 *  trips.txt            (IMPLEMENTED)
 *  stop_times.txt       (IMPLEMENTED)
 *  calendar.txt         (IMPLEMENTED)
 *  calendar_dates.txt   (IMPLEMENTED)
 *  fare_attributes.txt  (NOT IMPLEMENTED)
 *  fare_rules.txt       (NOT IMPLEMENTED)
 *  shapes.txt           (IMPLEMENTED)
 *  frequencies.txt      (IMPLEMENTED)
 *  transfers.txt        (NOT IMPLEMENTED)
 *  feed_info.txt        (NOT IMPLEMENTED)
 */

  def agencies: Seq[Agency]
  def stops: Seq[Stop]
  def routeRecords: Seq[RouteRecord]
  def tripRecords: Seq[TripRecord]
  def stopTimeRecords: Seq[StopTimeRecord]
  def calendarRecords: Seq[CalendarRecord]
  def calendarDateRecords: Seq[CalendarDateRecord]
  def tripShapes: Seq[TripShape]
  def frequencyRecords: Seq[FrequencyRecord]
}

object GtfsRecords {
  /** Reads GTFS records from a diretory containing GTFS records
    * that are based on the GTFS specification (Revised June 20, 2012)
    */
  def fromFiles(directory: String): GtfsRecords =
    io.csv.CsvGtfsRecords(directory)

  def fromDatabase(implicit session: Session): GtfsRecords =
    new io.database.DatabaseGtfsRecords
}
