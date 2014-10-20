package com.azavea.opentransit.indicators

import scala.annotation.tailrec

import com.azavea.gtfs._
import com.azavea.opentransit.CalculationStatus
import com.azavea.opentransit.CalculationStatus._

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

  def apply(periods: Seq[SamplePeriod], builder: TransitSystemBuilder, 
    overallGeometries: SystemGeometries, statusManager: CalculationStatusManager, 
    trackStatus:(String, CalculationStatus) => Unit): Unit = {

    try {
      val weeklyHours = new WeeklyServiceHours(periods, builder)

      val firstDay = weeklyHours.representativeWeekday.getOrElse({ 
        println("No representative weekday found!  Not calculating weekly service hours.")
        return
      })

      println("Representative weekday found; going to calculate weekly service hours.")
      trackStatus(name, CalculationStatus.Processing)

      val overallResults = weeklyHours.calculate(firstDay)
      val results: Seq[ContainerGenerator] = OverallIndicatorResult.createContainerGenerators(
        name, overallResults, overallGeometries)
      statusManager.indicatorFinished(results)
      
      trackStatus(name, CalculationStatus.Complete)
      println("Done processing weekly service hours!")
    } catch {
        case e: Exception => {
          println(e.getMessage)
          println(e.getStackTrace.mkString("\n"))
          statusManager.statusChanged(Map(name -> CalculationStatus.Failed))
        }
    }
  }
}

class WeeklyServiceHours(val periods: Seq[SamplePeriod], val builder: TransitSystemBuilder) {
  // Helper method to perform calculation (and ease testing).
  def calculate(firstDay: LocalDateTime): AggregatedResults = {
    val routeResults = hoursByRoute(buildSystems(buildWeek(firstDay)))
    val modeResults = routeResults.groupBy { case (route, results) => route.routeType }
      .map { case (route, results) => (route, results.values.max) }.toMap
    val systemResult = Some(routeResults.values.max)        
    AggregatedResults(routeResults, modeResults, systemResult)
  }

  // Get total system hours per route for the representative week.
  def hoursByRoute(weekSystems: Seq[TransitSystem]): Map[Route, Double] = {

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
    @tailrec
    def sumRouteHours(systems: Seq[TransitSystem], soFar: Map[Route, Double]): Map[Route, Double] = {
      if (systems.isEmpty) soFar
      else {
        val dayRoutes = systemRouteHours(systems.head)
        val newMap = 
          if (soFar.isEmpty) dayRoutes
          else soFar ++ dayRoutes.map{ case (k, v) => k -> (v + soFar.getOrElse(k, 0.0)) }
        sumRouteHours(systems.tail, newMap)
      }
    }

    sumRouteHours(weekSystems, Map[Route, Double]())
  }

  // Get the representative weekday by finding it from the sample periods.
  def representativeWeekday: Option[LocalDateTime] = {
    // Recursively look through sample periods and return the first one that's for a weekday.
    @tailrec def weekdayPeriod(samplePeriods: Seq[SamplePeriod]): Option[SamplePeriod] = {
      if (samplePeriods.isEmpty) None
      else {
        val firstPeriod = samplePeriods.head
        if (samplePeriodIsWeekday(firstPeriod)) Some(firstPeriod) 
        else weekdayPeriod(samplePeriods.tail)
      }
    }

    def samplePeriodIsWeekday(period: SamplePeriod): Boolean = {
      period.periodType != "alltime" && period.periodType != "weekend"
    }

    // start each day at midnight
    weekdayPeriod(periods) match {
      case Some(p) => Some(p.start.withTime(0, 0, 0, 0))
      case None => None
    }
  }

  // returns periods for each day of the week starting on the given date
  def buildWeek(startDateTime: LocalDateTime): Seq[SamplePeriod] = {
    // recursively build list of periods for each day in week
    @tailrec def buildDay(offset: Int, week: List[SamplePeriod]): Seq[SamplePeriod] = {
      if (offset == 7) week else {
        val startDay = startDateTime.plusDays(offset)
        buildDay(offset + 1, 
          week ++ List(SamplePeriod(offset, offset.toString, startDay, startDay.plusDays(1))))
      }
    }
    buildDay(0, List())
  }

  // get a transit system for each day of the representative week
  def buildSystems(days: Seq[SamplePeriod]): Seq[TransitSystem] =
    days.map(day => builder.systemBetween(day.start, day.end))
}