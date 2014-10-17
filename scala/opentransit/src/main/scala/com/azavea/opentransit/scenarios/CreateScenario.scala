package com.azavea.opentransit.scenarios

import scala.slick.jdbc.JdbcBackend.{Database, Session, DatabaseDef}
import com.azavea.gtfs._
import com.azavea.opentransit.JobStatus
import com.azavea.opentransit.JobStatus._

object CreateScenario {
  /** Creates a new scenario and passes status to a sink function.
    */
  def apply(request: ScenarioCreationRequest)(sink: JobStatus => Unit): Unit = {
    // TODO: create scenario instead of simulating with a sleep
    Thread.sleep(5000)
    sink(JobStatus.Complete)
  }
}
