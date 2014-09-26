package com.azavea.gtfs

import com.github.nscala_time.time.Imports._

/**
  * Represents the schedule of a single trip along a route
  */
trait Trip {
  def id: String
  def headsign: Option[String]
  def tripShape: Option[TripShape]
  def schedule: Seq[ScheduledStop]
}

object Trip {
  def apply(record: TripRecord, scheduledStops: Seq[ScheduledStop], tripShapes: String => TripShape): Trip =
    new Trip {
      def id = record.id
      def headsign = record.headsign
      def tripShape = record.tripShapeId.map(tripShapes(_))
      def schedule = scheduledStops
    }
}
