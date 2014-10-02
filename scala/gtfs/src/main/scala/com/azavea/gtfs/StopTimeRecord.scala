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
  arrivalTime: Period,
  departureTime: Period,
  distanceTraveled: Option[Double] = None
)
