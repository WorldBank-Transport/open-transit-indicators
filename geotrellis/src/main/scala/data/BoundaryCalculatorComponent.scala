package opentransitgt.data

import com.azavea.gtfs._
import scala.slick.driver.PostgresDriver
import geotrellis.vector._
import geotrellis.slick._
import opentransitgt.IndicatorCalculator

/**
 * A trait providing Boundaries to an IndicatorCalculator
 */
trait BoundaryCalculatorComponent {this: IndicatorCalculator =>

  // Wrap Slick persistence items to prevent potential naming conflicts.
  object bufSlick {
    val profile = PostgresDriver
    val gis = new PostGisProjectionSupport(profile)
  }
  import bufSlick.profile.simple._
  import bufSlick.gis._


  /**
   * A boundary, of either a city or a region.
   */
  case class Boundary(
    id: Int,
    geom: Projected[MultiPolygon] // Location-dependent SRID (UTM zone)
  )
 
  /**
   * Table class supporting Slick persistence
   */
  class Boundaries(tag: Tag) extends Table[Boundary](tag, "utm_datasources_boundary") {
    def id = column[Int]("boundary_id")
    def geom = column[Projected[MultiPolygon]]("geom")

    def * = (id, geom) <> (Boundary.tupled, Boundary.unapply)
  }

  def boundaryTable = TableQuery[Boundaries]
  /**
   * Returns a Boundary (geometry denoting a city or region boundary)
   */
  def boundaryWithId(boundaryId: Int): Option[Boundary] = {
    db withSession { implicit session: Session =>
      boundaryTable.filter(_.id === boundaryId).firstOption
    }
  }
}
