package opentransitgt.indicators

import com.azavea.gtfs._
import com.azavea.gtfs.data._
import geotrellis.vector._
import opentransitgt._
import opentransitgt.data._
import opentransitgt.DjangoAdapter._
import scala.slick.jdbc.JdbcBackend.DatabaseDef

// Areal Coverage Ratio of Transit Stops (user-configurable buffer)
class CoverageRatioStopsBuffer(val gtfsData: GtfsData, val calcParams: CalcParams, val db: DatabaseDef)
  extends IndicatorCalculator
  with BoundaryCalculatorComponent
  with StopsBufferCalculatorComponent
{
  val name = "coverage_ratio_stops_buffer"
  val bufferRadiusMeters = calcParams.nearby_buffer_distance_m

  // The following are just fake values because this is a system-wide indicator only
  def calcByMode(period: SamplePeriod): Map[Int, Double] = { Map() }

  def calcByRoute(period: SamplePeriod): Map[String, Double] = {
    Map()
  }

  // Only calculate by City boundary for now.
  def calcBySystem(period: SamplePeriod): Double = {
    val stopIds = stopIdsFromRoutes(routesInPeriod(period), period)
    val stopLocs = gtfsData.stops.filter(s => stopIds.contains(s.id))

    val systemBuffer = stopsBuffer(stopLocs)
    val cityBounds = boundary(calcParams.city_boundary_id)
    clippedAreaRatio(cityBounds.geom, systemBuffer.geom)
  }

  /**
   * Helper method to get all stop ids associated with a set of Routes.
   **/
  def stopIdsFromRoutes(routes: Seq[Route], period: SamplePeriod): Seq[StopId] = {
    routes.map{ route => tripsInPeriod(period, route)
      .map(_.stops).flatten.map(_.stop_id)
    }
    .flatten
  }

  /**
   * Intersects coverage with boundary and calculates the ratio of the resulting area.
   *
   */
  def clippedAreaRatio(boundary: MultiPolygon, coverage: MultiPolygon): Double = {
    ((boundary & coverage) match {
      case MultiPolygonResult(mp) => mp.area
      case PolygonResult(p) => p.area
      case _ => 0.0
    }) / boundary.area
  }
}
