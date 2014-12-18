package com.azavea.opentransit.indicators.parameters

import com.azavea.opentransit._
import com.azavea.opentransit.database._

import geotrellis.slick._
import geotrellis.vector._

import scala.slick.jdbc.JdbcBackend.DatabaseDef

trait RegionDemographics {
 def jobsDemographics: Seq[JobsDemographics]
}

case class JobsDemographics(geom: MultiPolygon, jobs: Double, population: Double) {
  def populationPerArea: Double = 
    population / geom.area
}

object RegionDemographics {
  def apply(db: DatabaseDef): RegionDemographics =
    new RegionDemographics {
      def jobsDemographics: Seq[JobsDemographics] =
        db withSession { implicit session =>
          DemographicsTable.regionDemographics.map { demographic =>
            JobsDemographics(demographic.geom.geom, demographic.destinationMetric1, demographic.populationMetric1)
          }
        }
    }
}

