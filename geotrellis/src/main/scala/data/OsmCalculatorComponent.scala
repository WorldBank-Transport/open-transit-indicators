package opentransitgt.data

import com.azavea.gtfs._
import scala.slick.driver.PostgresDriver
import geotrellis.vector._
import geotrellis.slick._
import opentransitgt.IndicatorCalculator
import grizzled.slf4j.Logging

/**
* A trait providing OSM data access to an IndicatorCalculator
*/
trait OSMCalculatorComponent extends Logging {this: IndicatorCalculator =>

  // Wrap Slick persistence items to prevent potential naming conflicts.
  object sbSlick {
    val profile = PostgresDriver
    val gis = new PostGisProjectionSupport(profile)
  }
  import sbSlick.profile.simple._
  import sbSlick.gis._


  /**
  * A multiline of all roads pulled from OSM.
  */
  case class Road(
    osm_id: String,
    geom: Projected[Line] // Location-dependent SRID (UTM zone)
  )

  /**
  * Table class supporting Slick persistence
  */
  class Roads(tag: Tag) extends Table[Road](tag, "planet_osm_roads") {
    def osm_id = column[String]("osm_id")
    def geom = column[Projected[Line]]("way")

    def * = (osm_id,geom) <> (Road.tupled, Road.unapply)
  }

  def roadTable = TableQuery[Roads]
  /**
  * Returns roadlines (geometry denoting a set of roads)
  */
  def allRoads: List[Road] = {
    debug("Inside OSMCalculatorComponent, gathering all roads")
    db withSession { implicit session: Session =>
      val roads = roadTable.list
      roads map {x =>
        Road(
          osm_id = x.osm_id,
          geom = x.geom.withSRID(4326)
        )
      }
    }
  }
}
