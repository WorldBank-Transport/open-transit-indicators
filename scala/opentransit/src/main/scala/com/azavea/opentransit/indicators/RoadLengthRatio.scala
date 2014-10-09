package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.opentransit._

import geotrellis.vector._

import com.github.nscala_time.time.Imports._
import org.joda.time._

// Areal Coverage Ratio of Transit Stops (user-configurable buffer)
class RoadLengthRatio(roadLength: RoadLength)
    extends Indicator
       with AggregatesBySystem {
  type Intermediate = Double

  val name = "lines_roads"

  val totalRoadLength = roadLength.totalRoadLength

  def calculation(period: SamplePeriod) = {

      def map(trips: Seq[Trip]): Double =
        trips
          .map { trip =>
            trip.tripShape.map { ts => ts.line.geom.length }
           }
          .flatten
          .foldLeft(0.0)(math.max(_, _))

      def reduce(stops: Seq[Double]): Double =
        stops.foldLeft(0.0)(_ + _) / totalRoadLength

      perRouteCalculation(map, reduce)
    }
}

/** The ratio of road length to transit system length
 *  TODO: replace the above code with this commented code once JTS is not broken
class RoadLengthRatio(roadLength: RoadLength)
    extends Indicator
       with AggregatesBySystem {
  type Intermediate = MultiLine

  val name = "lines_roads"

  val totalRoadLength = roadLength.totalRoadLength

  def calculation(period: SamplePeriod) = {
    def map(trips: Seq[Trip]): MultiLine =
      MultiLine {
        trips
          .map { trip =>
            trip.tripShape.map { ts => ts.line.geom }
          }
          .flatten
      }
      .union match {
        case MultiLineResult(ml) => ml
        case LineResult(l) => MultiLine(l)
        case _ => MultiLine.EMPTY
      }

    def reduce(routes: Seq[MultiLine]): Double = {
      val x = MultiLine(routes.map(_.lines).flatten)
        .union match {
          case MultiLineResult(ml) => ml
          case LineResult(l) => MultiLine(l)
          case _ => MultiLine.EMPTY
        }
        x.lines.map { line =>
          line.length
        }
        .foldLeft(0.0)(_ + _) / totalRoadLength
    }
    perRouteCalculation(map, reduce)
  }
}
 */
