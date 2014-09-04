package opentransitgt.indicators

import com.azavea.gtfs.StopDateTime
import com.azavea.gtfs.data._
import com.github.nscala_time.time.Imports._
import opentransitgt._
import opentransitgt.DjangoAdapter._
import scala.slick.jdbc.JdbcBackend.DatabaseDef

// Average Service Frequency
class AvgServiceFreq(val gtfsData: GtfsData, val calcParams: CalcParams, val db: DatabaseDef)
    extends IndicatorCalculator {

  val name = "avg_service_freq"

  /** This implicit class adds a function on Seq[StopDateTime] that calculates the headway */
  implicit class SeqStopDateTimeWrapper(stops: Seq[StopDateTime]) {
    def calculateHeadway(): Seq[Double] =
      stops
        .groupBy(_.stop_id)
        .mapValues { stops =>
          calcStopDifferences(stops.sortBy(_.arrival).toArray)
         }
        .values
        .toSeq
        .flatten
   }

  def calcByRoute(period: SamplePeriod): Map[String, Double] =
    routesInPeriod(period)
      .map { route =>
        val result =
          tripsInPeriod(period, route)
            .map(_.stops)
            .flatten
            .calculateHeadway
        (route.id,result)
       }
      .toMap
      .mapValues { stop_differences => stop_differences.sum / stop_differences.size }

  def calcByMode(period: SamplePeriod): Map[Int, Double] = {
    routesInPeriod(period).groupBy(_.route_type.id.toInt).mapValues(routes => {
      routes.map(tripsInPeriod(period, _)).flatten.map(trip => trip.stops).flatten
    }).mapValues(calculateHeadway).mapValues(diff_list => diff_list.sum / diff_list.size)
  }

  // Helper function to calculate headway for each stop + trip (hours per vehicle)
  def calculateHeadway(stops: Array[StopDateTime]) = {
    stops.groupBy(_.stop_id).mapValues(stops =>
      calcStopDifferences(stops.sortBy(_.arrival).toArray)).values.flatten
  }
}
