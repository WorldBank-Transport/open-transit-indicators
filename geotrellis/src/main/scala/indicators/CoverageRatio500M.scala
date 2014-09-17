package opentransitgt.indicators

import com.azavea.gtfs.data._
import geotrellis.vector._
import opentransitgt._
import opentransitgt.data._
import opentransitgt.DjangoAdapter._
import scala.slick.jdbc.JdbcBackend.DatabaseDef

// Areal Coverage Ratio of Transit Stops (500m buffer)
class CoverageRatio500M(val gtfsData: GtfsData, val calcParams: CalcParams, val db: DatabaseDef)
  extends IndicatorCalculator
  with BoundaryCalculatorComponent
  with StopsBufferCalculatorComponent
{
  val name = "coverage_ratio_500m"
  val bufferRadiusMeters = calcParams.nearby_buffer_distance_m

  // The following are just fake values because this is a system-wide indicator only
  def calcByMode(period: SamplePeriod): Map[Int, Double] = { Map(0 -> 0.0) }

  def calcByRoute(period: SamplePeriod): Map[String, Double] = { Map("Fake" -> 0.0) }

  // Only calculate by City boundary for now.
  def calcBySystem(period: SamplePeriod): Double = {
    val systemBuffer = stopsBuffer()
    val cityBounds: MultiPolygon = boundaryWithId(calcParams.city_boundary_id) match {
      case Some(boundary) => boundary.geom
      case None => MultiPolygon.EMPTY
    }
    clippedAreaRatio(cityBounds, systemBuffer.geom)
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
