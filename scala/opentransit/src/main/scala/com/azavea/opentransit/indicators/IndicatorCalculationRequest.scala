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

// Calculation request parameters
case class IndicatorCalculationRequest(
  token: String,
  id: Int,
  povertyLine: Double,
  nearbyBufferDistance: Double,
  arriveByTime: Int,
  maxCommuteTime: Int,
  cityBoundaryId: Int,
  regionBoundaryId: Int,
  averageFare: Double,
  gtfsDbName: String,
  auxDbName: String,
  samplePeriods: List[SamplePeriod],
  paramsRequirements: Requirements
) extends Logging
