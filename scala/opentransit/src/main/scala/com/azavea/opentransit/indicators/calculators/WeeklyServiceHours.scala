package com.azavea.opentransit.indicators

import scala.annotation.tailrec

import com.azavea.gtfs._
import com.azavea.opentransit.JobStatus
import com.azavea.opentransit.JobStatus._

import com.github.nscala_time.time.Imports._
import org.joda.time.Seconds

// This indicator calculation is too strange to inherit from Indicator, as it does not use sample 
// periods.  So, it gets its own processing logic here, to be run from CalculateIndicators after
// the other indicators are done.
object WeeklyServiceHours {
  val name = "hours_service"

  // helper function to ease running tests
  def runTest(periods: Seq[SamplePeriod], builder: TransitSystemBuilder): AggregatedResults = {
    val weeklyHours = new WeeklyServiceHours(periods, builder)
    val firstDay = weeklyHours.representativeWeekday.get
    weeklyHours.calculate(firstDay)
  }

  def apply(
    periods: Seq[SamplePeriod],
    builder: TransitSystemBuilder,
    overallGeometries: SystemGeometries,
    statusManager: CalculationStatusManager,
    trackStatus: (String, String, JobStatus) => Unit
  ): Unit = {

    try {
      val weeklyHours = new WeeklyServiceHours(periods, builder)

      val firstDay = weeklyHours.representativeWeekday.getOrElse({
        println("No representative weekday found!  Not calculating weekly service hours.")
        return
      })

      println("Representative weekday found; going to calculate weekly service hours.")
      trackStatus("alltime", name, JobStatus.Processing)

      val overallResults = weeklyHours.calculate(firstDay)
      val results: Seq[ContainerGenerator] = OverallIndicatorResult.createContainerGenerators(
        name, overallResults, overallGeometries)
      statusManager.indicatorFinished(results)

      println("Done processing weekly service hours!")
    } catch {
        case e: Exception => {
          println(e.getMessage)
          println(e.getStackTrace.mkString("\n"))
          trackStatus("alltime", name, JobStatus.Failed)
        }
    }
  }
}

class WeeklyServiceHours(val periods: Seq[SamplePeriod], val builder: TransitSystemBuilder) {
  // Helper method to perform calculation (and ease testing).
  def calculate(firstDay: LocalDateTime): AggregatedResults = {
    val routeResults = hoursByRoute(firstDay)
    val modeResults = routeResults.groupBy { case (route, results) => route.routeType }
      .map { case (route, results) => (route, results.values.max) }.toMap
    val systemResult = Some(routeResults.values.max)
    AggregatedResults(routeResults, modeResults, systemResult)
  }

  // Get total system hours per route for the representative week.
  def hoursByRoute(startDateTime: LocalDateTime): Map[Route, Double] = {
    // returns transit system for given day of the week within the representative week
    def buildDay(offset: Int = 0): TransitSystem = {
      if (offset >= 7) throw new IllegalArgumentException("Starting day must not be more than one week out.")
      else {
        val startDay = startDateTime.plusDays(offset)
        val period = SamplePeriod(offset, offset.toString, startDay, startDay.plusDays(1))
        builder.systemBetween(period.start, period.end)
      }
    }

    def systemRouteHours(system: TransitSystem): Map[Route, Double] =
      if (system.routes.isEmpty) Map[Route, Double]()
      else system.routes.map(route => route -> {
        routeHours(route) match {
          case Some(v) => Seconds.secondsBetween(v._1, v._2).getSeconds / 60.0 / 60.0
          case None => 0
        }
      }).toMap

    // returns earliest departure and latest arrival for given route
    def routeHours(route: Route): Option[(LocalDateTime, LocalDateTime)] = {

      // find the earliest departure and latest arrival across all the route's trips
      @tailrec 
      def tripHours(trips: Seq[Trip], lastMin: LocalDateTime, lastMax: LocalDateTime): 
        Option[(LocalDateTime, LocalDateTime)] = {

        if (trips.isEmpty) Some((lastMin, lastMax))
        else {
          val tripStops = trips.head.schedule
          val arrivals = tripStops.map(stop => stop.arrivalTime)
          val departures = tripStops.map(stop => stop.departureTime)
          val tripMin = if (departures.isEmpty) lastMin else departures.min
          val tripMax = if (arrivals.isEmpty) lastMax else arrivals.max
          tripHours(trips.tail, 
                    if (tripMin < lastMin) tripMin else lastMin, 
                    if (tripMax > lastMax) tripMax else lastMax)
        }
      }

      if(route.trips.isEmpty) None
      else {
        val firstTripStops = route.trips.head.schedule
        tripHours(route.trips.tail, firstTripStops.map(
          stop => stop.departureTime).min, firstTripStops.map(stop => stop.arrivalTime).max)
      }
    }

    // sum the daily service windows for each route to get weekly hours for route
    var hoursForRoutes = scala.collection.mutable.Map[Route, Double]()
    val dayRoutes = (0 to 6).foreach{ dayOffset => 
      val day = systemRouteHours(buildDay(dayOffset)) 
      day.foreach{ case (k, v) => {
        val newVal = v + hoursForRoutes.getOrElse(k, 0.0)
        hoursForRoutes.update(k, newVal)
        }
      }
    }
    hoursForRoutes.toMap // make immutable
  }

  // Get the representative weekday by finding it from the sample periods.
  def representativeWeekday: Option[LocalDateTime] = 
    SamplePeriod.getRepresentativeWeekday(periods)
      .map { date => date.toLocalDateTime(new LocalTime(0, 0)) }
}

