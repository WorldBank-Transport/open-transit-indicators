package com.azavea.gtfs

import com.github.nscala_time.time.Imports._

/**
 * Represents a stop time in a sequence of stops in a trip
 * @param arrival_time arrival time as offset from midnight on a given day
 * @param departure_time departure time as offset from midnight on a given day
 * @param shape_dist_traveled how much of the trip LineString has been traveled
 */
case class StopTimeRecord(
  stopId: String,
  tripId: String,
  sequence: Int,
  arrival: Period,
  departure: Period,
  distanceTraveled: Option[Double] = None
) {
  /** Use given date to calculate arrival and departure time */
  def toStopDateTime(dt: LocalDateTime, offset: Period = 0.seconds): StopDateTime = {
    new StopDateTime(this,
      dt + arrival - offset,
      dt + departure - offset
    )
  }

  def toStopDateTime(dt: LocalDate): StopDateTime =
    this.toStopDateTime(dt.toLocalDateTime(LocalTime.Midnight))

  /**
   * Travel time between departure from this stop and arrival at the other
   * Warning: This relationship is not symmetric and only valid for stops on the same trip
   */
  def timeTo(that: StopTimeRecord): Period = {
    if (that.tripId != tripId) throw new IllegalArgumentException("Can't compare stop times on different trips")
    that.arrival - this.departure
  }
}

trait ScheduledStop {
  def stop: Stop
  def arrival: LocalDateTime
  def departure: LocalDateTime
  def distanceTraveled: Option[Double] 
}

object ScheduledStop {
  def apply(record: StopTimeRecord, midnight: LocalDateTime, stops: Map[String, Stop]): ScheduledStop =
    new ScheduledStop {
      val stop = stops(record.stopId)
      val arrival = midnight + record.arrival
      val departure = midnight + record.departure
      def distanceTraveled = record.distanceTraveled
    }

  /** Create a ScheduledStop with this startTime and offset, where offset is the first arrival time
    * of the StopRecord in a sequence. This is used for generating ScheduledStops from FrequencyRecords.
    */
  def apply(record: StopTimeRecord, startTime: LocalDateTime, offset: Period, stops: Map[String, Stop]): ScheduledStop =
    new ScheduledStop {
      val stop = stops(record.stopId)
      val arrival = startTime + record.arrival - offset
      val departure = startTime + record.departure - offset
      def distanceTraveled = record.distanceTraveled
    }
}
