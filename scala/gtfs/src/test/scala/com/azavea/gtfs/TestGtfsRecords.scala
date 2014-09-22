package com.azavea.gtfs

import com.azavea.gtfs.io.csv._

import com.github.nscala_time.time.Imports._
import geotrellis.vector._
import geotrellis.slick._

class TestGtfsRecords extends GtfsRecords {
  val serviceId = "SR1"

  def agencies: Seq[Agency] = List(
    Agency("", "THE", "http://", "EST")
  )

  def stops = List(
    Stop("S1", "Stop 1", None, Point(0,0).withSRID(0)),
    Stop("S2", "Stop 2", None, Point(10,10).withSRID(0)),
    Stop("S3", "Stop 3", Some("Stop"), Point(10,20).withSRID(0))
  )

  def routeRecords = List(
    RouteRecord("R1","Route 1", "The one true route", Funicular)
  )

  def tripRecords = List(
    TripRecord("T1", serviceId, "R1", None, None)
  )

  def stopTimeRecords = List(
    StopTimeRecord("S1","T1", 1, 0.seconds, 1.minute),
    StopTimeRecord("S2","T1", 2, 10.minutes, 11.minutes),
    StopTimeRecord("S3","T1", 3, 15.minutes, 16.minutes)
  )

  def calendarRecords = List(
    CalendarRecord(
      serviceId,
      "20130101",
      "20140101",
      Array(true, true, true, true, true, false, false)
    )
  )

  def calendarDateRecords = List(
    CalendarDateRecord(serviceId, new LocalDate(2013,1,5), true) //Saturday
  )

  def frequencyRecords = List(
    FrequencyRecord("T1", 10.hours, 10.hours + 13.minutes, 300.seconds)
  )

  def tripShapes: Seq[TripShape] = Seq.empty
}
