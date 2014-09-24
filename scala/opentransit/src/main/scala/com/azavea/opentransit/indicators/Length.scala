package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.opentransit._

object Length extends Indicator
                 with AggregatesByAll {
  val name = "length"

  val calculation = 
    new PerRouteIndicatorCalculation[Double] {
      def map(trips: Seq[Trip]): Double =
        trips.foldLeft(0.0) { (maxLength, trip) =>
          trip.tripShape match {
            case Some(shape) =>
              val tripLength = shape.line.length / 1000
              math.max(maxLength, tripLength)
            case None =>
              maxLength
          }
        }

      def reduce(routeLengths: Seq[Double]): Double =
        routeLengths.foldLeft(0.0)(_ + _)
    }
}

// Transit system length
// class Length(val gtfsData: GtfsData, val calcParams: CalcParams, val db: DatabaseDef) extends IndicatorCalculator {
//   val name = "length"

//   def calcByRoute(period: SamplePeriod): Map[String, Double] = {
//     println("in calcByRoute for Length")
//     routesInPeriod(period).map(route =>
//       route.id.toString -> tripsInPeriod(period, route)
//         .foldLeft(0.0) {(max, trip) => trip.rec.shape_id match {
//           case None => max
//           case Some(shapeID) => {
//             gtfsData.shapesById.get(shapeID) match {
//               case None => max
//               case Some(tripShape) => {
//                 math.max(max, tripShape.line.length / 1000)
//               }
//             }
//           }
//         }
//       }
//     ).toMap
//   }

//   def calcByMode(period: SamplePeriod): Map[Int, Double] = {
//     println("in calcByMode for Length")
//     // get the transit length per route, group by route type, and sum all the lengths
//     calcByRoute(period).toList
//       .groupBy(kv => routeByID(kv._1).route_type.id)
//       .map { case (key, value) => key -> value.map(_._2).sum }
//   }

//   def calcBySystem(period: SamplePeriod): Double = simpleSumBySystem(period)
// }
