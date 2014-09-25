package com.azavea.gtfs

import com.azavea.opentransit.testkit._

import com.github.nscala_time.time.Imports._

import org.scalatest._

class TransitSystemBuilderSpec extends FunSpec with Matchers {
  describe("TransitSystemFactory") {
    it("should leave out stops in a trip if they are before or after the start and end date") {
      val builder = TransitSystemBuilder(TestGtfsRecords())
      val systemWithAllStops = 
        builder.systemBetween(new LocalDateTime(2014, 2, 3, 5, 0), new LocalDateTime(2014, 2, 3, 18, 0)) //Weekday
      val systemWithoutSomeStops = 
        builder.systemBetween(new LocalDateTime(2014, 2, 3, 6, 15), new LocalDateTime(2014, 2, 3, 18, 0)) //Weekday


      def getDepartureTimes(system: TransitSystem): Seq[LocalDateTime] =
        system
          .routes
          .map(_.trips)
          .flatten
          .find(_.id == "BUS_WEEKDAY_WestBus_1").get
          .schedule
          .map(_.departureTime)

      val departureTimesWith =
        getDepartureTimes(systemWithAllStops).toSet

      val departureTimesWithout =
        getDepartureTimes(systemWithoutSomeStops).toSet

      departureTimesWith.size should be (3)
      departureTimesWithout.size should be (2)

      val scheduledStop = departureTimesWith.diff(departureTimesWithout).toList.head
      scheduledStop should be (new LocalDateTime(2014, 2, 3, 6, 0) + TestGtfsRecords.times.busWaitTime)
    }

    it("should not include a route with no trips, and include one with trips") {
      val records = GtfsRecords.fromFiles(TestFiles.septaPath)
      val builder = TransitSystemBuilder(records)
      val start = new LocalDateTime("2014-06-01T00:00:00.000")
      val end = new LocalDateTime("2014-06-01T08:00:00.000")
      builder.systemBetween(start, end).routes.find(_.id == "GLN") should be (None)
      builder.systemBetween(start, end).routes.find(_.id == "AIR") should not be (None)
    }

    it("should return an empty system on dates where no services are specified to be running") {
      val records = GtfsRecords.fromFiles(TestFiles.septaPath)
      val builder = TransitSystemBuilder(records)
      val start = new LocalDateTime("1950-01-01T00:00:00.000")
      val end = new LocalDateTime("1950-01-01T08:00:00.000")
      val system = builder.systemBetween(start, end)
      system.routes.size should be (0)
    }
  }
}
