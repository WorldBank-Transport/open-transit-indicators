package com.azavea.gtfs

import com.github.nscala_time.time.Imports._

/**
 * Trip that is scheduled to happen on a concrete date and time
 *
 * @param rec trips GTFS record that was used to generate this trip
 * @param stops list of stop times that have been resolved to specific date and time
 */
class ScheduledTrip (
  val rec: TripRecord,
  val stops: Array[StopDateTime]
) {
  def trip_id = rec.id
  def starts: LocalDateTime = stops.head.arrival
  def ends: LocalDateTime = stops.last.departure
}
