package com.azavea.opentransit.indicators.parameters

import com.azavea.opentransit._
import com.azavea.opentransit.database.DemographicsTable

import geotrellis.slick._
import geotrellis.vector._

import scala.slick.jdbc.JdbcBackend.DatabaseDef

import com.typesafe.config.ConfigFactory

trait Demographics {
  def jobsDemographics: Seq[MultiPolygonFeature[Int]] = 
    regionDemographics(Demographics.jobsColumnName)

  def populationMetricForBuffer(buffer: Projected[MultiPolygon], columnName:String): Double
  def regionDemographics(columnName: String): Seq[MultiPolygonFeature[Int]]
}

object Demographics {
  val config = ConfigFactory.load
  val jobsColumnName = config.getString("opentransit.demographics.jobs-column-name")

  def apply(db: DatabaseDef): Demographics =
    new Demographics {
      def populationMetricForBuffer(buffer: Projected[MultiPolygon], columnName:String): Double =
        db withSession { implicit session =>
          DemographicsTable.getPopMetric(buffer, columnName)
        }

      def regionDemographics(columnName: String): Seq[MultiPolygonFeature[Int]] =
        ???
    }
}
