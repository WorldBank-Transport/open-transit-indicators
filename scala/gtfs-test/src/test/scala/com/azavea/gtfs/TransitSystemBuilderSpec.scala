package com.azavea.gtfs

import com.azavea.opentransit.testkit._

import com.github.nscala_time.time.Imports._

import org.scalatest._

class TransitSystemFactorySpec extends FunSpec with Matchers {
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
  }

  it("asdf") {

    def combineMaps(m: Seq[Map[Int, Int]]): Map[Int, Seq[Int]] = {
      m.map(_.toSeq).flatten.groupBy(_._1).mapValues(_.map(_._2))
    }

    val in = Seq( Map( 1 -> 2, 2 -> 4), Map(2 -> 5) )
    val m = combineMaps(in)

    m should be (Map(1 -> Seq(2), 2 -> Seq(4, 5)))
  }
}
