package com.azavea.gtfs

import scala.util.Try
import com.github.nscala_time.time.Imports._

trait ScheduledStop {
  def stop: Stop
  def arrivalTime: LocalDateTime
  def departureTime: LocalDateTime
  def distanceTraveled: Option[Double]

  override
  def toString: String = 
    s"SHEDULED(stop = ${stop.name}, arrivelTime = $arrivalTime, departureTile = $departureTime)"
}

object ScheduledStop {
  def apply(record: StopTimeRecord, midnight: LocalDateTime, stops: StopId => Stop): ScheduledStop =
    new ScheduledStop {
      val stop = stops(record.stopId)
      val arrivalTime = midnight + record.arrivalTime
      val departureTime = midnight + record.departureTime
      def distanceTraveled = record.distanceTraveled
    }

  /** Create a ScheduledStop with this startTime and offset, where offset is the first arrival time
    * of the StopRecord in a sequence. This is used for generating ScheduledStops from FrequencyRecords.
    */
  def apply(record: StopTimeRecord, startTime: LocalDateTime, offset: Period, stops: StopId => Stop): Option[ScheduledStop] =
    Try(stops(record.stopId)).toOption match {
      case Some(stop) => Some {
        new ScheduledStop {
          val stop = stops(record.stopId)
          val arrivalTime = startTime + record.arrivalTime - offset
          val departureTime = startTime + record.departureTime - offset
          def distanceTraveled = record.distanceTraveled
        }
      }
      case None => None
    }
}
