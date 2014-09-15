package opentransitgt.indicators

import com.azavea.gtfs.data._
import opentransitgt._
import opentransitgt.DjangoAdapter._
import scala.slick.jdbc.JdbcBackend.DatabaseDef

// Transit system length
class Length(val gtfsData: GtfsData, val calcParams: CalcParams, val db: DatabaseDef) extends IndicatorCalculator {
  val name = "length"

  def calcByRoute(period: SamplePeriod): Map[String, Double] = {
    println("in calcByRoute for Length")
    routesInPeriod(period).map(route =>
      route.id.toString -> tripsInPeriod(period, route)
        .foldLeft(0.0) {(max, trip) => trip.rec.shape_id match {
          case None => max
          case Some(shapeID) => {
            gtfsData.shapesById.get(shapeID) match {
              case None => max
              case Some(tripShape) => {
                math.max(max, tripShape.line.length / 1000)
              }
            }
          }
        }
      }
    ).toMap
  }

  def calcByMode(period: SamplePeriod): Map[Int, Double] = {
    println("in calcByMode for Length")
    // get the transit length per route, group by route type, and sum all the lengths
    calcByRoute(period).toList
      .groupBy(kv => routeByID(kv._1).route_type.id)
      .map { case (key, value) => key -> value.map(_._2).sum }
  }

  def calcBySystem(period: SamplePeriod): Double = simpleSumBySystem(period)
}
