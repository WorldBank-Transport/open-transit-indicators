package com.azavea.gtfs

import com.github.nscala_time.time.Imports._
import scala.util.Try

/**
  * Represents the schedule of a single trip along a route
  */
trait Trip {
  def id: String
  def headsign: Option[String]
  def tripShape: Option[TripShape]
  def schedule: Seq[ScheduledStop]

  override
  def toString: String =
    s"Trip(id = $id, headsign = $headsign)"
}

object Trip {
  def apply(record: TripRecord, scheduledStops: Seq[ScheduledStop], tripShapes: String => TripShape): Trip =
    new Trip {
      def id = record.id
      def headsign = record.headsign
      def tripShape = Try(tripShapes(record.tripShapeId.get)).toOption
      def schedule = scheduledStops
    }
}
