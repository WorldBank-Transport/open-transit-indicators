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

  // The following are explicitly not implemented because this indicator is
  // system-wide only.
  def calcByMode(period: SamplePeriod): Map[Int, Double] = ???
  def calcByRoute(period: SamplePeriod): Map[String, Double] = ???

  // This punts on calculating by both city and region boundaries but makes
  // it easy to implement later.
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
    boundary.area / ((boundary & coverage) match {
      case MultiPolygonResult(mp) => mp.area
      case PolygonResult(p) => p.area
    })
  }
}
