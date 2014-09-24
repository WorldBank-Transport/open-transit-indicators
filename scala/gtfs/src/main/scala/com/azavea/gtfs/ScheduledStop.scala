package com.azavea.gtfs

import com.github.nscala_time.time.Imports._

trait ScheduledStop {
  def stop: Stop
  def arrivalTime: LocalDateTime
  def departureTime: LocalDateTime
  def distanceTraveled: Option[Double]
}

object ScheduledStop {
  def apply(record: StopTimeRecord, midnight: LocalDateTime, stops: Map[String, Stop]): ScheduledStop =
    new ScheduledStop {
      val stop = stops(record.stopId)
      val arrivalTime = midnight + record.arrivalTime
      val departureTime = midnight + record.departureTime
      def distanceTraveled = record.distanceTraveled
    }

  /** Create a ScheduledStop with this startTime and offset, where offset is the first arrival time
    * of the StopRecord in a sequence. This is used for generating ScheduledStops from FrequencyRecords.
    */
  def apply(record: StopTimeRecord, startTime: LocalDateTime, offset: Period, stops: Map[String, Stop]): ScheduledStop =
    new ScheduledStop {
      val stop = stops(record.stopId)
      val arrivalTime = startTime + record.arrivalTime - offset
      val departureTime = startTime + record.departureTime - offset
      def distanceTraveled = record.distanceTraveled
    }
}
