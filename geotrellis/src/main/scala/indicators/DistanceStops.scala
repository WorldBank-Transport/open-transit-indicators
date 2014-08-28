package opentransitgt.indicators

import com.azavea.gtfs.data._
import opentransitgt._
import opentransitgt.DjangoAdapter._

// Average distance between stops
class DistanceStops(val gtfsData: GtfsData, val calcParams: CalcParams) extends IndicatorCalculator {
  val name = "distance_stops"

  def calcByRoute(period: SamplePeriod): Map[String, Double] = {
    val gotRoutes = routesInPeriod(period)
    gotRoutes.map(route =>
      route.id.toString -> {
        val gotTrips = tripsInPeriod(period, route)
        
        // for each route, get tuple of:
        // (sum of trip shape lengths, maximum number of stops in any trip)
        val sumLengthAndMaxStops = gotTrips
            .foldLeft((0.0, 0.0)) {(sumLengthAndMaxStops, trip) => trip.rec.shape_id match {
            case None => sumLengthAndMaxStops
            case Some(shapeID) => {
              gtfsData.shapesById.get(shapeID) match {
                case None => sumLengthAndMaxStops
                case Some(tripShape) => {
                  (sumLengthAndMaxStops._1 + tripShape.line.length, 
                  math.max(sumLengthAndMaxStops._2, trip.stops.size))
                }
              }
            }
          }
        }
        // per route, calculate:
        // average length of trips in km / max # of legs in any trip (total stops, -1)
        ((sumLengthAndMaxStops._1 / 1000) / gotTrips.size) / (sumLengthAndMaxStops._2 - 1)
      }
    ).toMap
  }

  def calcByMode(period: SamplePeriod): Map[Int, Double] = {
    // get the average distance between stops per route, group by route type, 
    // and average all the distances
    calcByRoute(period).toList
      .groupBy(kv => routeByID(kv._1).route_type.id)
      .mapValues(v => v.map(_._2).sum / v.size)
  }
}
