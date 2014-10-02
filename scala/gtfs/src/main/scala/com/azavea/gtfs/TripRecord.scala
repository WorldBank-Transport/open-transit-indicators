package com.azavea.gtfs

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
