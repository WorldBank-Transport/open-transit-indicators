package opentransitgt.indicators

import com.azavea.gtfs.data._
import opentransitgt._
import opentransitgt.DjangoAdapter._

// Number of routes
class NumRoutes(val gtfsData: GtfsData, val calcParams: CalcParams) extends IndicatorCalculator {
  val name = "num_routes"

  def calcByRoute(period: SamplePeriod): Map[String, Double] = {
    // this calculation isn't very interesting by itself, but when aggregated,
    // it shows the average amount of time where the route is available at all.
    routesInPeriod(period).map(route =>
      (route.id.toString -> 1.0)
    ).toMap
  }

  def calcByMode(period: SamplePeriod): Map[Int, Double] = {
    // get all routes, group by route type, and count the size of each group
    routesInPeriod(period)
      .groupBy(_.route_type.id)
      .mapValues(_.size)
  }
}
