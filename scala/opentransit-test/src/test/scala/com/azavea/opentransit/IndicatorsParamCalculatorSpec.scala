package com.azavea.opentransit.indicators

import geotrellis.vector._
import com.azavea.gtfs._
import com.azavea.gtfs.io.csv._
import com.azavea.opentransit.io.GtfsIngest

import com.azavea.opentransit._
import com.azavea.opentransit.indicators.parameters._
import com.azavea.opentransit.testkit._

import com.github.nscala_time.time.Imports._
import com.typesafe.config.{ConfigFactory,Config}

import org.scalatest._

import scala.slick.jdbc.JdbcBackend.Session
import scala.util.{Try, Success, Failure}


/** This fixture simulates an actual request and allows the usage of params in
 *  a fashion quite similar to that used in production. This makes it more brittle
 *  (which is good) but also less granular (which is bad) than alternative approaches.
 *
 *  This is a good place to look to familiarize yourself with the pipeline
 *  that calculations actually go through in the production application.
 *  Extra comments included to better explain the inner-workings of the application.
 */
trait AdHocE2EParamSpec extends DatabaseTestFixture { self: Suite =>

  // This is a mock request which IndicatorsRoute.scala generates from an incoming
  // POST
  val requestSamplePeriods: List[SamplePeriod]

  val request = IndicatorCalculationRequest(
      token = "AuthToken",
      version = "HashToKeepGTFSSystemsVersioned",
      povertyLine = 30000,
      nearbyBufferDistance = 500,
      maxCommuteTime = 30,
      maxWalkTime = 10,
      cityBoundaryId = 1, // index in DB
      regionBoundaryId = 1, // index in DB
      averageFare = 2.5,
      samplePeriods = requestSamplePeriods,
      runAccessibility = false
  )

  // Create GTFS records
  val scheduledRecords: GtfsRecords
  val observedRecords: GtfsRecords


  /** This is the GTFS Parser's TransitSystemBuilder, which takes
   *  appropriately parsed GTFS records and produces a transit system
   *  for our use (typically it will then be subsetted out by its
   *  `.systemBetween` method)
   */
  val scheduledSystemBuilder = TransitSystemBuilder(scheduledRecords)
  val observedSystemBuilder = TransitSystemBuilder(observedRecords)

  // Pull samplePeriods from our request and subset out our TransitSystem
  val periods = request.samplePeriods
  val scheduledSystemsByPeriod =
    periods.map { period =>
      (period -> scheduledSystemBuilder.systemBetween(period.start, period.end))
    }.toMap
  val observedSystemsByPeriod =
    periods.map { period =>
      (period -> scheduledSystemBuilder.systemBetween(period.start, period.end))
    }.toMap

  /** `params` is the object you'll reference to grab parameter values in indicators
   *  It is also the Object which brings together the various calculations that are
   *  shared by multiple indicator calculation jobs.
   *  Any data that needs to be mocked should be done by overrides on this.
   */
  class params extends IndicatorParams {
    // For our purposes, overriding the values is the best plan of action
    val overrides = new NonGtfsTestExtensions {
      val scheduledSystem: TransitSystem = scheduledSystem
    }
    override val cityBoundary = overrides.cityBoundary
    override val regionBoundary = overrides.regionBoundary
    override def totalRoadLength = overrides.totalRoadLength
    override def populationMetricForBuffer(
        buffer: geotrellis.slick.Projected[geotrellis.vector.MultiPolygon],
        columnName: String
    ): Double = 100
    override def observedForTrip(
        period: com.azavea.opentransit.indicators.SamplePeriod,
        scheduledTripId: String
    ): com.azavea.gtfs.Trip = observedSystemsByPeriod.values.head.routes.head.trips.head
    override val settings: com.azavea.opentransit.indicators.parameters.IndicatorSettings =
      IndicatorSettings(
          request.povertyLine,
          request.nearbyBufferDistance,
          request.maxCommuteTime,
          request.maxWalkTime,
          request.averageFare,
          request.runAccessibility
      )
    override def bufferForPeriod(
        period: com.azavea.opentransit.indicators.SamplePeriod
    ): geotrellis.slick.Projected[geotrellis.vector.MultiPolygon] =
      geotrellis.slick.Projected(geotrellis.vector.MultiPolygon.EMPTY, 4326)
    override def bufferForStop(stop: com.azavea.gtfs.Stop): geotrellis.vector.Polygon =
      Polygon(Line((1,1),(2,2),(1,2),(1,1)))
  }

  // Aliases for use in tests
  val firstPeriod = scheduledSystemsByPeriod.keys.head
  val firstSystem = scheduledSystemsByPeriod.values.head


  // helper functions for testing
  /** This helper function is potentially tricky:
   *  just remember to instantiate the implicit it assumes.
   *  This instantiation should be as simple as:
   *  `implicit val routeMap = byRoute`
   */
  def routeById(routeId: String)(implicit routeMap: Map[Route, Double]): Double = {
    val routeIdMap = routeMap.map { case (k, v) => (k.id -> v) }
    Try(routeIdMap(routeId)) match {
      case Success(answer) => answer
      case Failure(e) => sys.error("No route matching that route ID")
    }
  }
}


