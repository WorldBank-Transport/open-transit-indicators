package com.azavea.gtfs

import com.github.nscala_time.time.Imports._

import scala.collection.mutable

/**
 * Service availability calendar for a set of dates, combining both regular service and exceptions
 * Source: calendar.txt, calendar_dates.txt
 */
object ServiceCalendar {
  def apply(dates: Seq[LocalDate], calendarRecords: Seq[CalendarRecord], calendarDateRecords: Seq[CalendarDateRecord]): Map[LocalDate, String => Boolean] = {
    dates
      .map { date =>
        // Represents what services are active or not on this date.
        val schedule =  mutable.Map[String, Boolean]()

        // Set active services for the weekly schedue on this date, if it exists.
        for(record <- calendarRecords) {
          if(record.start <= date && date <= record.end) {
            if(record.activeOn(date)) {
              schedule(record.serviceId) = true
            }
          }
        }

        // Set the addition or removal of services for service exceptions on this date.
        for(record <- calendarDateRecords) {
          if(record.date == date) {
            schedule(record.serviceId) = record.serviceAdded
          }
        }

        val f: String => Boolean = 
          { tripId: String =>
            schedule.get(tripId) match {
              case Some(bool) => bool
              case None => false
            }
          }
        (date, f)
       }
      .toMap
  }
}
