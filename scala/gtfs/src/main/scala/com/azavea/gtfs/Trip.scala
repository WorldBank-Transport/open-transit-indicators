package com.azavea.gtfs

import com.github.nscala_time.time.Imports._
import scala.Some
import scala.collection.mutable
import scala.collection.mutable.{ListBuffer, ArrayBuffer}
import geotrellis.vector._

/**
 * An abstract trip, detailing the sequence and time of the stops but not the date
 *
 * @param service_id service calendar to which this trip is subject
 * @param route_id route to which this trip belongs
 * @param trip_headsign
 */
case class TripRecord (
  id: String,
  serviceId: ServiceId,
  routeId: RouteId,
  headsign: Option[String] = None,
  tripShapeId: Option[String] = None
)

/**
  * Represents the schedule of a single trip along a route
  */
trait Trip {
  def headsign: Option[String]
  def tripShape: Option[TripShape]
  def schedule: Seq[ScheduledStop]
}

object Trip {
  def apply(record: TripRecord, scheduledStops: Seq[ScheduledStop], tripShapes: String => TripShape): Trip =
    new Trip {
      def headsign = record.headsign
      def tripShape = record.tripShapeId.map(tripShapes(_))
      def schedule = scheduledStops
    }
}
