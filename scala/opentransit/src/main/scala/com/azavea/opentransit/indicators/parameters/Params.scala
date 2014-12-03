package com.azavea.opentransit.indicators.parameters

import com.azavea.gtfs._

import com.azavea.opentransit._
import com.azavea.opentransit.database._
import com.azavea.opentransit.indicators._

import geotrellis.slick._
import geotrellis.vector._

import scala.slick.jdbc.JdbcBackend.{Database, DatabaseDef, Session}


case class IndicatorSettings(
  povertyLine: Double,
  nearbyBufferDistance: Double,
  maxCommuteTime: Int,
  arriveByTime: Int,
  averageFare: Double,
  hasDemographics: Boolean,
  hasOsm: Boolean,
  hasObserved: Boolean,
  hasCityBounds: Boolean,
  hasRegionBounds: Boolean,
  hasJobDemographics: Boolean
)

// Do not change by period or scenario
trait StaticParams {
  val settings: IndicatorSettings
}

trait IndicatorParams extends Boundaries
                         with StopBuffers
                         with RoadLength
                         with StaticParams
                         with Demographics
                         with ObservedStopTimes
object IndicatorParams {
  def apply(
    request: IndicatorCalculationRequest,
    system: TransitSystem,
    period: SamplePeriod,
    dbByName: String => Database): IndicatorParams = {

    // Grab references to each of the databases, so they can be used as needed
    val gtfsDb = dbByName(request.gtfsDbName)
    val auxDb = dbByName(request.auxDbName)

    // Stop buffers are based on stops, which are in the gtfs db
    val stopBuffers = gtfsDb withSession { implicit session =>
      StopBuffers(system, request.nearbyBufferDistance, gtfsDb)
    }

    // Observed data and demographics are in the aux db
    val observedStopTimes = ObservedStopTimes(system, period, auxDb, request.paramsRequirements.observed)
    val demographics = Demographics(auxDb)

    new IndicatorParams {
      def observedStopsByTrip(tripId: String) =
        observedStopTimes.observedStopsByTrip(tripId)
      def observedTripById(tripId: String) =
        observedStopTimes.observedTripById(tripId)
      def missingTripData: Seq[String] =
        observedStopTimes.missingTripData

      def bufferForStop(stop: Stop): Projected[MultiPolygon] = stopBuffers.bufferForStop(stop)
      def bufferForStops(stops: Seq[Stop]): Projected[MultiPolygon] = stopBuffers.bufferForStops(stops)
      def bufferForPeriod(period: SamplePeriod): Projected[MultiPolygon] =
        stopBuffers.bufferForPeriod(period)


      def populationMetricForBuffer(buffer: Projected[MultiPolygon], columnName: String) =
        demographics.populationMetricForBuffer(buffer, columnName)

      val settings =
        IndicatorSettings(
          request.povertyLine,
          request.nearbyBufferDistance,
          request.maxCommuteTime,
          request.arriveByTime,
          request.averageFare,
          hasDemographics = request.paramsRequirements.demographics,
          hasOsm = request.paramsRequirements.osm,
          hasObserved = request.paramsRequirements.observed,
          hasCityBounds = request.paramsRequirements.cityBounds,
          hasRegionBounds = request.paramsRequirements.regionBounds,
          hasJobDemographics = request.paramsRequirements.jobDemographics
        )

      // Boundaries and OSM data -- all in the aux db
      val (cityBoundary, regionBoundary, totalRoadLength) = auxDb withSession { implicit session =>
        (Boundaries.cityBoundary(request.cityBoundaryId),
          Boundaries.cityBoundary(request.regionBoundaryId),
          RoadLength.totalRoadLength)
      }
    }
  }
}
