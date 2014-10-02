package com.azavea.gtfs

import com.github.nscala_time.time.Imports._

/**
 * Represents a weekly service calendar between two dates
 * Source: calendar.txt
 *
 * @param service_id unique service identifier
 * @param start_date starting date of service (inclusive)
 * @param end_date ending date of service (inclusive)
 * @param week Array representing service availability on each day of the week
 */
case class CalendarRecord(
  serviceId: String,
  start: LocalDate,
  end: LocalDate,
  week: Array[Boolean]
) {
  require(serviceId != "", "Service ID is required")
  require(start <= end, "Time must flow forward")
  require(week.length == 7, "Week must contain 7 days")

  def activeOn(dt: LocalDate): Boolean = week(dt.getDayOfWeek - 1)
  def activeOn(weekDay: Int): Boolean = week(weekDay - 1)

  override
  def hashCode =
    (serviceId, start, end, (week(0), week(1), week(2), week(3), week(4), week(5), week(6))).hashCode

  override
  def equals(obj: Any): Boolean =
    obj match {
      case o: CalendarRecord =>
        (o.serviceId, o.start, o.end, (o.week(0), o.week(1), o.week(2), o.week(3), o.week(4), o.week(5), o.week(6))) ==
          (serviceId, start, end, (week(0), week(1), week(2), week(3), week(4), week(5), week(6)))
    }
}
