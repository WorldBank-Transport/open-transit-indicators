package com.azavea.gtfs

import com.github.nscala_time.time.Imports._

import org.scalatest._

class ServiceCalendarSpec extends FunSpec with Matchers {
  val data = new TestGtfsRecords  

  val sunday = new LocalDate(2013,1,6)
  val monday = new LocalDate(2013,1,7)
  //this date is an exception, S1 does not run on weekends
  val saturday = new LocalDate(2013,1,5)


  describe("ServiceCalendar") {
    it("should return active services based on CalendarRecords") {
      val cal =
        ServiceCalendar(Seq(sunday, monday), data.calendarRecords, data.calendarDateRecords)
      cal(monday)(data.serviceId) should equal (true)
      cal(sunday)(data.serviceId) should equal (false)
    }

    it("should return active service based on CalendarDateRecord") {
      val cal =
        ServiceCalendar(Seq(saturday, sunday, monday), data.calendarRecords, data.calendarDateRecords)

      saturday.getDayOfWeek should equal (6)
      cal(saturday)(data.serviceId) should equal (true)
    }
  }
}
