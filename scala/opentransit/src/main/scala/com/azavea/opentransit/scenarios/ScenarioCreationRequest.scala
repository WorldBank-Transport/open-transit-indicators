package com.azavea.opentransit.scenarios

import com.azavea.opentransit.indicators.SamplePeriod
import geotrellis.vector._
import grizzled.slf4j.Logging
import scala.slick.jdbc.JdbcBackend.Session

// Scenario creation request parameters
case class ScenarioCreationRequest(
  token: String,
  dbName: String,
  baseDbName: String,
  samplePeriod: SamplePeriod
) extends Logging
