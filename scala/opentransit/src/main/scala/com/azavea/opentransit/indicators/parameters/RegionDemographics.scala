package com.azavea.opentransit.indicators.parameters

import com.azavea.opentransit._
import com.azavea.opentransit.database._

import geotrellis.slick._
import geotrellis.vector._

import scala.slick.jdbc.JdbcBackend.DatabaseDef

trait RegionDemographics {
 def jobsDemographics: Seq[MultiPolygonFeature[Double]] =
    regionDemographics { demographic =>
      // How we're dealing with the demographic columns is
      // not very type safe.
      MultiPolygonFeature(demographic.geom.geom, demographic.destinationMetric1)
    }

  def regionDemographics(featureFunc: RegionDemographic => MultiPolygonFeature[Double]): Seq[MultiPolygonFeature[Double]]
}

object RegionDemographics {
  def apply(db: DatabaseDef): RegionDemographics =
    new RegionDemographics {
      def regionDemographics(featureFunc: RegionDemographic => MultiPolygonFeature[Double]): Seq[MultiPolygonFeature[Double]] = 
        db withSession { implicit session =>
          DemographicsTable.regionDemographics.map(featureFunc)
        }
    }
}
