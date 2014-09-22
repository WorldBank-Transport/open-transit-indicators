package com.azavea.gtfs.io.database

import com.azavea.gtfs._

import scala.slick.jdbc.JdbcBackend.Session

class DatabaseGtfsRecords(implicit session: Session) 
    extends GtfsRecords 
       with GtfsTables {
  import profile.simple._

  def agencies: Seq[Agency] = agenciesTable.list
  def stops: Seq[Stop] = stopsTable.list
  def routeRecords: Seq[RouteRecord] = routeRecordsTable.list
  def tripRecords: Seq[TripRecord] = tripRecordsTable.list
  def stopTimeRecords: Seq[StopTimeRecord] = stopTimeRecordsTable.list
  def calendarRecords: Seq[CalendarRecord] = calendarRecordsTable.list
  def calendarDateRecords: Seq[CalendarDateRecord] = calendarDateRecordsTable.list
  def tripShapes: Seq[TripShape] = tripShapesTable.list
  def frequencyRecords: Seq[FrequencyRecord] = frequencyRecordsTable.list
}
