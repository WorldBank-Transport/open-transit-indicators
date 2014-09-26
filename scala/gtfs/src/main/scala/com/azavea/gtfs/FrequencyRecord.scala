package com.azavea.gtfs

import com.github.nscala_time.time.Imports._

/**
 * Represents a repetition of a trip in a time with headway
 * @param trip_id Related trip
 * @param start_time The time of the first trip as offset from midnight
 * @param end_time No trips may leave after this time as offset from midnight
 * @param headway delay between the start of the trips
 */
case class FrequencyRecord (
  tripId: String,
  start: Period,
  end: Period,
  headway: Duration
) {
  assert(start != null)
  assert(end != null)

  def generateStartTimes(dt: LocalDate): Iterator[LocalDateTime] =
    new Iterator[LocalDateTime] {
      var s = dt.toLocalDateTime(LocalTime.Midnight) + start
      val e   = dt.toLocalDateTime(LocalTime.Midnight) + end

      override def hasNext: Boolean = s <= e

      override def next(): LocalDateTime = {
        val ret = s
        s += headway
        ret
      }
    }
}
