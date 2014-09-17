package opentransitgt.data

import com.azavea.gtfs._
import scala.slick.driver.PostgresDriver
import geotrellis.vector._
import geotrellis.slick._
import opentransitgt.IndicatorCalculator

/**
 * A buffer around a set of GTFS stops. Used for calculating stop coverage indicators
 */
case class StopsBuffer(
  radius: Double,
  geom: Projected[MultiPolygon], // Location-dependent SRID (UTM zone)
  theGeom: Projected[MultiPolygon] // SRID EPSG:4326
)

/**
 * A trait providing a stops buffer to an IndicatorCalculator
 */
trait StopsBufferCalculatorComponent {this: IndicatorCalculator =>
  import PostgresDriver.simple._
  private val gisSupport = new PostGisProjectionSupport(PostgresDriver)
  import gisSupport._

  val bufferRadiusMeters: Double

  /**
   * Table class supporting Slick persistence
   */
  class StopsBuffers(tag: Tag) extends Table[StopsBuffer](tag, "gtfs_stops_buffers") {
    def radius = column[Double]("radius_m")
    def geom = column[Projected[MultiPolygon]]("geom")
    def theGeom = column[Projected[MultiPolygon]]("the_geom")

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
    // TODO: The indicator spec specifies that if stops data is unavailable, the route lines
    // will be buffered instead. This doesn't appear to be supported by the GTFS parser yet.
    db withSession { implicit session: Session =>
      val srid = gtfsData.stops(0).geom.srid
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
          val newBuffer = StopsBuffer(bufferRadiusMeters,
            bufferMp.withSRID(srid),
            bufferMp.withSRID(4326))
          stopsBufferTable.insert(newBuffer)
          newBuffer
      }
    }
  }
}
