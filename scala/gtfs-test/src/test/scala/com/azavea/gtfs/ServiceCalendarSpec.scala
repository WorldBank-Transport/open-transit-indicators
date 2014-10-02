package com.azavea.gtfs

import com.azavea.gtfs.io.csv._

import com.github.nscala_time.time.Imports._

import org.scalatest._

class ServiceCalendarSpec extends FunSpec with Matchers {
  val sunday = new LocalDate(2013,1,6)
  val monday = new LocalDate(2013,1,7)
  //this date is an exception, S1 does not run on weekends
  val saturday = new LocalDate(2013,1,5)

  describe("ServiceCalendar") {
    val serviceId = "SR1"

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

    it("should return active services based on CalendarRecords") {
      val cal =
        ServiceCalendar(Seq(sunday, monday), calendarRecords, calendarDateRecords)
      cal(monday)(serviceId) should equal (true)
      cal(sunday)(serviceId) should equal (false)
    }

    it("should return active service based on CalendarDateRecord") {
      val cal =
        ServiceCalendar(Seq(saturday, sunday, monday), calendarRecords, calendarDateRecords)

      saturday.getDayOfWeek should equal (6)
      cal(saturday)(serviceId) should equal (true)
    }
  }
}
