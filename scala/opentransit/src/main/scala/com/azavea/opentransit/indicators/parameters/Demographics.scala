package com.azavea.opentransit.indicators.parameters

import com.azavea.opentransit._
import com.azavea.opentransit.database.DemographicsTable

import geotrellis.slick._
import geotrellis.vector._

import scala.slick.jdbc.JdbcBackend.DatabaseDef

trait Demographics {
  def populationMetricForBuffer(buffer: Projected[MultiPolygon], columnName:String): Double
}

object Demographics {
  def apply(db: DatabaseDef): Demographics = {
    def getPopulationMetric(buffer: Projected[MultiPolygon], columnName:String): Double =
      db withSession {implicit session =>
        DemographicsTable.getPopMetric(buffer, columnName)
      }
    new Demographics {
      def populationMetricForBuffer(buffer: Projected[MultiPolygon], columnName:String): Double = {
        getPopulationMetric(buffer, columnName)
      }
    }
  }
}
