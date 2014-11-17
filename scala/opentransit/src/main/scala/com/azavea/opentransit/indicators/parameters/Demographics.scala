package com.azavea.opentransit.indicators.parameters

import com.azavea.opentransit._
import com.azavea.opentransit.database._

import geotrellis.slick._
import geotrellis.vector._

import scala.slick.jdbc.JdbcBackend.DatabaseDef

trait Demographics {
  def populationMetricForBuffer(buffer: Projected[MultiPolygon], columnName:String): Double
}

object Demographics {
  def apply(db: DatabaseDef): Demographics =
    new Demographics {
      def populationMetricForBuffer(buffer: Projected[MultiPolygon], columnName:String): Double =
        db withSession { implicit session =>
          DemographicsTable.getPopMetric(buffer, columnName)
        }

      def regionDemographics(featureFunc: RegionDemographic => MultiPolygonFeature[Double]): Seq[MultiPolygonFeature[Double]] = 
        db withSession { implicit session =>
          DemographicsTable.regionDemographics.map(featureFunc)
        }
    }
}
