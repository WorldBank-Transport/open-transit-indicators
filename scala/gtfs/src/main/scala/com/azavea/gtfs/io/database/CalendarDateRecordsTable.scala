package com.azavea.gtfs.io.database

import com.azavea.gtfs._
import com.github.nscala_time.time.Imports._

trait CalendarDateRecordsTable { this: Profile =>
  import profile.simple._
  import joda._

  class CalendarDateRecords(tag: Tag) extends Table[CalendarDateRecord](tag, this.datesTableName) {
    def service_id = column[String]("service_id")
    def date = column[LocalDate]("date")
    def exception_type = column[Int]("exception_type")

    def * = (service_id, date, exception_type) <> (applyCalendarDateRecord _, unapplyCalendarDateRecord _)

    def applyCalendarDateRecord(row: (String, LocalDate, Int)): CalendarDateRecord = {
      val (id, date, t) = row
      CalendarDateRecord(id, date, t == 1)
    }

    def unapplyCalendarDateRecord(record: CalendarDateRecord): Option[(String, LocalDate, Int)] =
      Some( (record.serviceId, record.date, if (record.serviceAdded) 1 else 2) )
  }

  val calendarDateRecordsTable = TableQuery[CalendarDateRecords]
}

