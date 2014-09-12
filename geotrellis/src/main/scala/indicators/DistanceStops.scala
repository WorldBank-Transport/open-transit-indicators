package opentransitgt.indicators

import com.azavea.gtfs.data._
import opentransitgt._
import opentransitgt.DjangoAdapter._
import scala.slick.jdbc.JdbcBackend.DatabaseDef

// Average distance between stops
class DistanceStops(val gtfsData: GtfsData, val calcParams: CalcParams, val db: DatabaseDef) extends IndicatorCalculator {
  val name = "distance_stops"

  def calcByRoute(period: SamplePeriod): Map[String, Double] =
    // for each route, get tuple of:
    // (sum of trip shape lengths, maximum number of stops in any trip)
    routesInPeriod(period)
      .map { route =>
        val (totalLength, maxStops, count) = 
            tripsInPeriod(period, route)
              .foldLeft((0.0, 0.0, 0.0)) { (acc, trip) => 
                  val (totalLength, maxStops, count) = acc
                  trip.rec.shape_id match {
                    case None => (totalLength, maxStops, count + 1)
                    case Some(shapeID) => {
                      gtfsData.shapesById.get(shapeID) match {
                        case None => (totalLength, maxStops, count + 1)
                        case Some(tripShape) =>
                         (totalLength + tripShape.line.length, 
                         math.max(maxStops, trip.stops.size),
                         count + 1)

                  }
                }
              }
            }

        // per route, calculate:
        // average length of trips in km / max # of legs in any trip (total stops, -1)
        val result = 
          ((totalLength / 1000) / count) / (maxStops - 1)
      
        (route.id.toString, result)
      }
     .toMap


  def calcByMode(period: SamplePeriod): Map[Int, Double] = {
    println("in calcByMode for DistanceStops")
    // get the average distance between stops per route, group by route type, 
    // and average all the distances
    calcByRoute(period).toList
      .groupBy(kv => routeByID(kv._1).route_type.id)
      .map { case (key, value) => key -> value.map(_._2).sum / value.size }
  }
}
