package com.azavea.opentransit.database

import com.azavea.gtfs._
import com.azavea.opentransit.indicators._

import com.github.nscala_time.time.Imports._

import geotrellis.slick._
import geotrellis.vector._

import scala.slick.driver.{JdbcDriver, JdbcProfile, PostgresDriver}
import scala.slick.jdbc.{StaticQuery => Q}


trait IndicatorsTable {
  import PostgresDriver.simple._
  private val gisSupport = new PostGisProjectionSupport(PostgresDriver)
  import gisSupport._

  def indicatorResultToTuple(result: IndicatorResultContainer): Option[(String, Int, String,
    Double, Projected[Geometry], Int, String, Int, Boolean)] = {
    val aggregationString = result.aggregation match {
      case RouteAggregate => "route"
      case RouteTypeAggregate => "mode"
      case SystemAggregate => "system"
    }
    val routeId = result.routeType match {
      case Some(t) => t.id
      case None => 0
    }
    Option(result.indicatorId, result.samplePeriod.id, aggregationString,
      result.value, Projected(result.geom, 4326), result.calculationJob,
      result.routeId, routeId, result.cityBounded)
  }

  // TODO: Fix Me! This is a dummy implementation that actually will not work; however,
  // seems to be necessary. To make this work, a real sample period needs to be constructed
  // from a given SamplePeriodId
  def indicatorResultFromTuple(input: (String, Int, String,
    Double, Projected[Geometry], Int, String, Int, Boolean)): IndicatorResultContainer = {

    // Fix Me: Construct an actual sample period
    def dummySamplePeriod(samplePeriodId: Int) = {
      SamplePeriod(samplePeriodId, "alltime",
        new LocalDateTime("01-01-01T00:00:00.000"),
        new LocalDateTime("2014-05-01T08:00:00.000")
      )
    }
    input match {
      case (indicatorId, samplePeriodId, aggregationString,
        value, geom, calculationJob, routeId, routeType, cityBounded) => {
        new IndicatorResultContainer(indicatorId, dummySamplePeriod(samplePeriodId), SystemAggregate, value,
          geom, calculationJob, routeId, Option(RouteType(routeType.toInt)), cityBounded)
      }
    }
  }

  class Indicators(tag: Tag) extends Table[IndicatorResultContainer](tag, "transit_indicators_indicator") {
    def indicatorId = column[String]("type")
    def samplePeriodType = column[Int]("sample_period_id")
    def aggregation = column[String]("aggregation")
    def value = column[Double]("value")
    def geom = column[Projected[Geometry]]("the_geom")
    def calculationJob = column[Int]("calculation_job_id")
    def routeId = column[String]("route_id")
    def routeType = column[Int]("route_type")
    def cityBounded = column[Boolean]("city_bounded")

    def * = (indicatorId, samplePeriodType, aggregation,
      value, geom, calculationJob, routeId, routeType, cityBounded) <> (indicatorResultFromTuple,
       indicatorResultToTuple)
  }

  def indicatorsTable = TableQuery[Indicators]

}
