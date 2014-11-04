package com.azavea.opentransit.indicators.calculators

import com.azavea.gtfs._
import com.azavea.opentransit._
import com.azavea.opentransit.indicators._
import com.azavea.opentransit.indicators.parameters._

import geotrellis.vector._

import com.github.nscala_time.time.Imports._
import org.joda.time._

// Ratio of transit system length to road length
class RatioLinesRoads(roadLength: RoadLength)
    extends Indicator
       with AggregatesBySystem {
  type Intermediate = MultiLine

  val name = "lines_roads"

  val totalRoadLength = roadLength.totalRoadLength

  def calculation(period: SamplePeriod) = {
      def map(trip: Trip): MultiLine =
        trip.tripShape match {
          case Some(ts) => MultiLine(ts.line)
          case None => MultiLine()
        }


      def reduce(mls: Seq[MultiLine]): Double = {
        val transitLength =
          mls.
            map(_.lines)
            .flatten
            .dissolve
            .map(_.length.toDouble)
            .sum / 1000 // m to km

        transitLength / totalRoadLength
      }

      perTripCalculation(map, reduce)
    }
}
