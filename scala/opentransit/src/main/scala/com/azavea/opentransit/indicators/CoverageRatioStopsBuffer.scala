package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.opentransit._

import geotrellis.vector._

import com.github.nscala_time.time.Imports._
import org.joda.time._

// Areal Coverage Ratio of Transit Stops (user-configurable buffer)
class CoverageRatioStopsBuffer(params: IndicatorCalculationParams)
    extends Indicator
       with AggregatesBySystem {
  type Intermediate = Seq[Stop]

  val name = "coverage_ratio_stops_buffer"

  val bufferRadius = params.nearbyBufferDistance
  val boundary = params.cityBoundary

  val calculation =
    new PerTripIndicatorCalculation[Seq[Stop]] {
      def map(trip: Trip): Seq[Stop] =
        trip.schedule.map(_.stop)

      def reduce(stops: Seq[Seq[Stop]]): Double = {
        val coverage =
          stops
            .flatten
            .map(_.point.geom.buffer(bufferRadius))
            .foldLeft(MultiPolygon.EMPTY) { (mp, p) =>
              mp | p match {
                case MultiPolygonResult(mp) => mp
                case PolygonResult(p) => MultiPolygon(p)
              }
            }

        ((boundary & coverage) match {
          case MultiPolygonResult(mp) => mp.area
          case PolygonResult(p) => p.area
          case _ => 0.0
        }) / boundary.area
      }
    }
}
