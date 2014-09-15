package opentransitgt.data_access

import com.azavea.gtfs._
import scala.slick.driver.PostgresDriver
import geotrellis.vector._
import geotrellis.slick._
import opentransitgt.IndicatorCalculator

/**
 * A trait providing a stops buffer to an IndicatorCalculator
 */
trait StopsBufferCalculatorComponent {this: IndicatorCalculator =>
  def bufferRadiusMeters: Float

  // Wrap Slick persistence items to prevent potential naming conflicts.
  object sbSlick {
    val profile = PostgresDriver
    val gis = new PostGisProjectionSupport(profile)
  }
  import sbSlick.profile.simple._
  import sbSlick.gis._


  /**
   * A buffer around a set of GTFS stops. Used for calculating stop coverage indicators
   */
  case class StopsBuffer(
    radius: Float,
    geom: Projected[GeometryCollection], // Location-dependent SRID (UTM zone)
    theGeom: Projected[GeometryCollection] // SRID EPSG:4326
  )
  
  /**
   * Table class supporting Slick persistence
   */
  class StopsBuffers(tag: Tag) extends Table[StopsBuffer](tag, "gtfs_stops_buffers") {
    def radius = column[Float]("radius_m")
    def geom = column[Projected[GeometryCollection]]("geom")
    def theGeom = column[Projected[GeometryCollection]]("the_geom")

    def * = (radius, geom, theGeom) <> (StopsBuffer.tupled, StopsBuffer.unapply)
  }

  def stopsBufferTable = TableQuery[StopsBuffers]
  /**
   * Returns a StopsBuffer (union of buffered Stops)
   * Attempts to get the StopsBuffer from the database; if that fails, constructs a new one
   * and immediately save it in the database. There should only be one StopsBuffer per GTFS
   * system, although if there are multiple rows in the table, it's not a problem since they
   * should all get constructed the same way. The gtfs_stops_buffers table should be deleted
   * along with the other GTFS tables on deletion.
   */
  def stopsBuffer(): StopsBuffer = {
    db withSession { implicit session: Session =>
      val srid = systemSRIDFixMe()
      stopsBufferTable.firstOption match {
        case Some(sb) => sb
        case None => 
          val bufferMp = gtfsData.stops.map(stop => stop.geom.buffer(bufferRadiusMeters))
            .foldLeft(MultiPolygon.EMPTY) {
              (union, geom) => union.union(geom) match { 
                case MultiPolygonResult(mp) => mp
                case PolygonResult(p) => MultiPolygon(p)
              }
            }
          val unprojected = GeometryCollection(Seq(bufferMp))
          val newBuffer = StopsBuffer(bufferRadiusMeters,
            unprojected.withSRID(srid),
            unprojected.withSRID(4326))
          stopsBufferTable.insert(newBuffer)
          newBuffer
      }
    }
  }
}
