package com.azavea.gtfs

import com.github.nscala_time.time.Imports._

/**
 * A stop on trip at specific date and time
 * @param rec GTFS stoptime record used to generate this stop
 * @param arrival date and time of arrival
 * @param departure date and time of departure
 */
class StopDateTime(
  rec: StopTimeRecord,
  val arrival: LocalDateTime,
  val departure: LocalDateTime
) {
  def stop_id: StopId = rec.stopId
  def sequence: Int = rec.sequence
}
