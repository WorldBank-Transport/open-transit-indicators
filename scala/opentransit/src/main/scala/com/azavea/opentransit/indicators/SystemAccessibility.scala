package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.opentransit._

import geotrellis.vector._

class SystemAccessibility(params: IndicatorCalculationParams)
    extends Indicator
       with AggregatesBySystem {
  type Intermediate = Seq[Stop]

  val name = "system_access"

  val bufferRadius = params.nearbyBufferDistance

  val calculation = ???
  /*
    new PerTripIndicatorCalculation[Seq[Stop]] {
      def map(trip: Trip): Seq[Stop] =
        trip.schedule.map(_.stop)


      def reduce(stops: Seq[Seq[Stop]]): Double = {
        // Get all stops, create buffer
        val coverage = stops
          .flatten
          .map(_.point.geom.buffer(bufferRadius))
          .foldLeft(MultiPolygon.EMPTY) { (mp, p) =>
          mp | p match {
            case MultiPolygon(mp) => mp
            case PolygonResult(p) => MultiPolygon(p)
          }
        }

      }
    }
   */

}
