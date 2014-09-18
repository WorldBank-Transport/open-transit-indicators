package opentransitgt.indicators

import com.azavea.gtfs._
import com.azavea.gtfs.data._
import opentransitgt._
import opentransitgt.DjangoAdapter._
import scala.slick.jdbc.JdbcBackend.DatabaseDef
import com.github.nscala_time.time.Imports._

// Number of hours in service for the week starting on the representative weekday selected
class WeeklyServiceHours(val gtfsData: GtfsData, val calcParams: CalcParams,
                         val db: DatabaseDef) extends IndicatorCalculator {

  val name = "hours_service"
  
  // get the representative weekday by finding it from the sample periods
  def representativeWeekday: LocalDate = {
    /** Recursively look through sample periods and return the first one that's for a weekday.
     *  This will throw ElementNotFoundException if no weekday sample periods found.
     *  (Should only run weekly service hours calculation with weekday sample period.)
     */
    def weekdayPeriod(samplePeriods: List[SamplePeriod]): SamplePeriod = {
      val firstPeriod = samplePeriods.head
      if (samplePeriodIsWeekday(firstPeriod)) firstPeriod else weekdayPeriod(samplePeriods.tail)
    }
    
    def samplePeriodIsWeekday(period: SamplePeriod): Boolean = {
      if (period.`type` != "alltime" && period.`type` != "weekend") true else false
    }

    weekdayPeriod(calcParams.sample_periods.toList).period_start.toLocalDate
  }
  
  // get service hours for a given day and route
  def serviceHoursForDay(day: LocalDate, route: Route): Double = {
    // Importing the context within this scope adds additional functionality to Routes
    import gtfsData.context._

    // returns number of hours in service window for given set of trips
    def serviceWindow(trips: Seq[ScheduledTrip], start: LocalDateTime, stop: LocalDateTime): Double = {
      if (trips.isEmpty) {
        val hrs = hoursDifference(start, stop)
        // do not return more than 24 hours of service for a day (if last trip goes past midnight)
        if (hrs > 24) 24 else hrs
      } else {
        val trip = trips.head
        val newStart = if (trip.starts < start) trip.starts else start
        val newStop = if (trip.ends > stop) trip.ends else stop
        serviceWindow(trips.tail, newStart, newStop)
      }
    }
    
    // getScheduledTripsOn returns Seq[ScheduledTrip] for a RichRoute
    val tripsForRoute = route.getScheduledTripsOn(day)
    if (tripsForRoute.isEmpty) 0 else serviceWindow(tripsForRoute.tail, 
                                                    tripsForRoute.head.starts,
                                                    tripsForRoute.head.ends)
  }
  
  // sum the hours for the week for the given route
  def serviceHoursForWeek(route: Route): Double = {
    val startDay = representativeWeekday

    def getHoursForWeekday(offset: Int, runningTotal: Double): Double = {
      if (offset == 7) {
        runningTotal
      } else
        getHoursForWeekday(offset + 1,
                           runningTotal + serviceHoursForDay(startDay.plusDays(offset),
                           route))
    }

    getHoursForWeekday(0, 0)
  }
  
  def calcByRoute(period: SamplePeriod): Map[String, Double] = {
    println("in calcByRoute for WeeklyServiceHours")
    routesInPeriod(period).map(route => route.id.toString -> serviceHoursForWeek(route)).toMap
  }
  
  def calcByMode(period: SamplePeriod): Map[Int, Double] = {
    println("in calcByMode for WeeklyServiceHours")
    calcByRoute(period).toList
      .groupBy(kv => routeByID(kv._1).route_type.id)
      .map { case (key, value) => key -> listMax(value.map(_._2).toList) }
  }
  
  def calcBySystem(period: SamplePeriod): Double = {
    println("in calcBySystem for WeeklyServiceHours")
    simpleMaxBySystem(period)
  }
}
