package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.opentransit.io.GtfsIngest

import com.azavea.opentransit.testkit._

import geotrellis.vector._
import geotrellis.vector.json._

import com.github.nscala_time.time.Imports._
import com.typesafe.config.{ConfigFactory,Config}

import org.scalatest._

import scala.slick.jdbc.JdbcBackend.Session
import scala.util.{Try, Success, Failure}

class SystemGeometriesTests extends FunSuite with Matchers with IndicatorSpec {
  test("shouldn't fail on db data for per-period creation") {
    Timer.timedTask("Calculating System Geometries") {
      val geoms =
        periods.map { period =>
          Timer.timedTask(s"Creating transit system for period $period") {
            val system = systemBuilder.systemBetween(period.start, period.end)
          }
          Timer.timedTask(s"Calculating System Geometries for period $period") {
            SystemGeometries(system)
          }
        }
      val overall =
        Timer.timedTask(s"Calculating overall System Geometries.") {
          SystemGeometries.merge(geoms)
        }
    }
  }
}
