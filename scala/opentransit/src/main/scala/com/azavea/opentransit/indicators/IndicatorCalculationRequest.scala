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
    city_bounds: Boolean,
    region_bounds: Boolean
)

// implicit conversion from json to above class
object RequirementsJsonProtocol extends DefaultJsonProtocol {
  implicit val requiresFormat = jsonFormat5(Requirements)
}

// Calculation request parameters
case class IndicatorCalculationRequest(
    token: String,
    version: String,
    povertyLine: Double,
    nearbyBufferDistance: Double,
    maxCommuteTime: Int,
    maxWalkTime: Int,
    cityBoundaryId: Int,
    regionBoundaryId: Int,
    averageFare: Double,
    samplePeriods: List[SamplePeriod],
    params_requirements: Requirements
) extends Logging
