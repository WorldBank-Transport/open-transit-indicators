package com.azavea.opentransit.indicators

import spray.json._
import geotrellis.vector._

import com.azavea.opentransit.database.{ BoundariesTable, RoadsTable }
import scala.slick.jdbc.JdbcBackend.Session

import grizzled.slf4j.Logging

case class Requirements(
  demographics: Boolean,
  osm: Boolean,
  observed: Boolean,
  cityBounds: Boolean,
  regionBounds: Boolean,
  jobDemographics: Boolean
)

// Change startTime to arriveByTime
case class TravelshedRequest(
  /** Resolution of the travelshed raster, in meters */
  resolution: Double,
  /** What time you would start travelling, in seconds from midnight */
  startTime: Int,
  /** Maximum travel duration, in seconds */
  duration: Int
)

// Calculation request parameters
case class IndicatorCalculationRequest(
  token: String,
  id: Int,
  povertyLine: Double,
  nearbyBufferDistance: Double,
  maxCommuteTime: Int,
  maxWalkTime: Int,
  cityBoundaryId: Int,
  regionBoundaryId: Int,
  averageFare: Double,
  gtfsDbName: String,
  auxDbName: String,
  samplePeriods: List[SamplePeriod],
  paramsRequirements: Requirements,
  travelshed: TravelshedRequest
) extends Logging
