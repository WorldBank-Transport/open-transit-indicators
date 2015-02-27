package com.azavea.opentransit.indicators

import scala.annotation.tailrec

import com.azavea.gtfs._
import com.azavea.opentransit.JobStatus
import com.azavea.opentransit.JobStatus._

import com.github.nscala_time.time.Imports._
import org.joda.time.Seconds

object JobAccess {
  def run(v: Option[Double], g: SystemGeometries) {
    val jobAccess = AggregatedResults.systemOnly {
      v match {
        case Some(x) => x
        case _ => 0
      }
    }
    OverallIndicatorResult.createContainerGenerators("Job Access", jobAccess, g)
  }
}
