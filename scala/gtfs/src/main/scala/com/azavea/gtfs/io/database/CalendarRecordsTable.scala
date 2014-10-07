package com.azavea.gtfs.io.database

import com.azavea.gtfs._
import com.github.nscala_time.time.Imports._

trait CalendarRecordsTable { this: Profile =>
  import profile.simple._
  import joda._

  class CalendarRecords(tag: Tag)
    extends Table[CalendarRecord](tag, this.calendarTableName)
  {
    def serviceId = column[String]("service_id", O.PrimaryKey)
    def start_date = column[LocalDate]("start_date")
    def end_date = column[LocalDate]("end_date")
    def monday = column[Int]("monday")
    def tuesday = column[Int]("tuesday")
    def wednesday = column[Int]("wednesday")
    def thursday = column[Int]("thursday")
    def friday = column[Int]("friday")
    def saturday = column[Int]("saturday")
    def sunday = column[Int]("sunday")

    def * = (serviceId, start_date, end_date, monday, tuesday, wednesday, thursday, friday, saturday, sunday) <>
      (applyCalendarRecord _, unapplyCalendarRecord _)

    def applyCalendarRecord(row: (String, LocalDate, LocalDate, Int, Int, Int, Int, Int, Int, Int)): CalendarRecord = {
      val (sid, from, to, mon, tue, wed, th, fri, sat, sun) = row
      CalendarRecord(sid, from, to, Array[Int](mon, tue, wed,th, fri, sat, sun).map(_ == 1))
    }

    def unapplyCalendarRecord(record: CalendarRecord): Option[(String, LocalDate, LocalDate, Int, Int, Int, Int, Int, Int, Int)] = {
      val w = record.week.map{ if (_) 1 else 0 }
      Some((record.serviceId, record.start, record.end, w(0), w(1), w(2), w(3), w(4), w(5), w(6)))
    }
  }

  val calendarRecordsTable = TableQuery[CalendarRecords]
}
