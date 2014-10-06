package com.azavea.opentransit.indicators

import geotrellis.vector._

import com.azavea.opentransit.database.{ BoundariesTable, RoadsTable }
import scala.slick.jdbc.JdbcBackend.Session

import grizzled.slf4j.Logging

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
  samplePeriods: List[SamplePeriod]
) extends Logging
