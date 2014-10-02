package com.azavea.gtfs

import com.github.nscala_time.time.Imports._

/**
  * Represents an exception to the regular service on given date
  * Source: calendar_dates.txt
  * @param service_id
  * @param date Date of service
  * @param serviceAdded  if this record indicates that service is added, this is true;
  *                      if this record indicates that serivce is removed, this is false
  */
case class CalendarDateRecord(
  serviceId: ServiceId,
  date: LocalDate,
  serviceAdded: Boolean
)
