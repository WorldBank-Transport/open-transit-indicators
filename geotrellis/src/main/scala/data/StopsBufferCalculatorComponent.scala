package opentransitgt.data

import com.azavea.gtfs._
import scala.slick.driver.PostgresDriver
import geotrellis.vector._
import geotrellis.slick._
import geotrellis.proj4.CRS
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
   * Constructs a stops buffer from the passed Seq[Stops] and then saves it to the database
   * so that it can later be displayed on the map (and deletes any existing stops buffers from
   * the database).
   * */
  def stopsBuffer(stops: Seq[Stop]): StopsBuffer = {
    // TODO: The indicator spec specifies that if stops data is unavailable, the route lines
    // will be buffered instead. This doesn't appear to be supported by the GTFS parser yet.
    db withSession { implicit session: Session =>
      val srid = gtfsData.stops(0).geom.srid
      val bufferMp = stops.map(stop => stop.geom.buffer(bufferRadiusMeters))
        .foldLeft(MultiPolygon.EMPTY) {
          (union, geom) => union.union(geom) match {
            case MultiPolygonResult(mp) => mp
            case PolygonResult(p) => MultiPolygon(p)
          }
        }
      
      val newBuffer = StopsBuffer(bufferRadiusMeters, bufferMp.withSRID(srid),
        bufferMp.withSRID(srid).reproject(CRS.fromName(s"EPSG:${srid}"),
                                          CRS.fromName("EPSG:4326"))(4326))
      // Clear out any existing buffers before inserting the new one.
      stopsBufferTable.delete
      stopsBufferTable.insert(newBuffer)
      newBuffer
    }
  }
}
