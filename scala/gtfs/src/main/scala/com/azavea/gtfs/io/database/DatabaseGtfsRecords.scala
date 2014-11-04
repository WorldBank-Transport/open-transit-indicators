package com.azavea.gtfs.io.database

import com.azavea.gtfs._

import scala.slick.jdbc.JdbcBackend.Session

class DatabaseGtfsRecords(implicit session: Session) 
    extends GtfsRecords 
       with GtfsTables { self: Profile =>
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

  def force: GtfsRecords =
    new GtfsRecords {
      val agencies: Seq[Agency] = DatabaseGtfsRecords.this.agencies
      val routeRecords: Seq[RouteRecord] = DatabaseGtfsRecords.this.routeRecords
      val tripRecords: Seq[TripRecord] = DatabaseGtfsRecords.this.tripRecords
      val stopTimeRecords: Seq[StopTimeRecord] = DatabaseGtfsRecords.this.stopTimeRecords
      val calendarRecords: Seq[CalendarRecord] = DatabaseGtfsRecords.this.calendarRecords
      val calendarDateRecords: Seq[CalendarDateRecord] = DatabaseGtfsRecords.this.calendarDateRecords
      val tripShapes: Seq[TripShape] = DatabaseGtfsRecords.this.tripShapes
      val frequencyRecords: Seq[FrequencyRecord] = DatabaseGtfsRecords.this.frequencyRecords
      val stops: Seq[Stop] = DatabaseGtfsRecords.this.stops
    }
}
