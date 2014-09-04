package opentransitgt.indicators

import com.azavea.gtfs.data._
import opentransitgt._
import opentransitgt.DjangoAdapter._
import scala.slick.jdbc.JdbcBackend.DatabaseDef

// Transit system length
class Length(val gtfsData: GtfsData, val calcParams: CalcParams, val db: DatabaseDef) extends IndicatorCalculator {
  val name = "length"

  def calcByRoute(period: SamplePeriod): Map[String, Double] = {
    routesInPeriod(period).map(route =>
      route.id.toString -> tripsInPeriod(period, route)
        .foldLeft(0.0) {(max, trip) => trip.rec.shape_id match {
          case None => max
          case Some(shapeID) => {
            gtfsData.shapesById.get(shapeID) match {
              case None => max
              case Some(tripShape) => {
                math.max(max, tripShape.line.length)
              }
            }
          }
        }
      }
    ).toMap
  }

  def calcByMode(period: SamplePeriod): Map[Int, Double] = {
    // get the transit length per route, group by route type, and average all the lengths
    calcByRoute(period).toList
      .groupBy(kv => routeByID(kv._1).route_type.id)
      .mapValues(v => v.map(_._2).sum / v.size)
  }
}
