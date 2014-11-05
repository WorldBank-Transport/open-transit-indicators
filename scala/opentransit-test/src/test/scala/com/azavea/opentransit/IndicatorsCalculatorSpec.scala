package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.gtfs.io.csv._
import com.azavea.opentransit.io.GtfsIngest
import com.azavea.opentransit.indicators.parameters._

import geotrellis.vector._
import geotrellis.slick._

import com.azavea.opentransit.testkit._

import com.github.nscala_time.time.Imports._
import com.typesafe.config.{ConfigFactory,Config}

import org.scalatest._

import scala.slick.jdbc.JdbcBackend.Session
import scala.util.{Try, Success, Failure}


trait IndicatorSpec extends DatabaseTestFixture { self: Suite =>
  /** It's horrible to load the data for each test. But I'm done pulling my hair
    * out trying to fix weird NullPointerExceptions and complaints that multiple
    * users are using the database. TODO: Not this.
    */
  val records = {
    val config = ConfigFactory.load
    val dbGeomNameUtm = config.getString("database.geom-name-utm")

    // Import the files into the database to do the reprojection.
    db withSession { implicit session =>
      val records = GtfsRecords.fromFiles(TestFiles.septaPath)
      GtfsIngest(records)
      GtfsRecords.fromDatabase(dbGeomNameUtm).force
    }
  }
  val observedRecords = CsvGtfsRecords(TestFiles.rtSeptaPath)

  val systemBuilder = TransitSystemBuilder(records)
  val observedSystemBuilder = TransitSystemBuilder(observedRecords)

  val periods =
    Seq(
      SamplePeriod(1, "night",
        new LocalDateTime("2014-05-01T00:00:00.000"),
        new LocalDateTime("2014-05-01T08:00:00.000")),

      SamplePeriod(1, "morning",
        new LocalDateTime("2014-05-01T08:00:00.000"),
        new LocalDateTime("2014-05-01T11:00:00.000")),

      SamplePeriod(1, "midday",
        new LocalDateTime("2014-05-01T11:00:00.000"),
        new LocalDateTime("2014-05-01T16:30:00.000")),

      SamplePeriod(1, "evening",
        new LocalDateTime("2014-05-01T16:30:00.000"),
        new LocalDateTime("2014-05-01T23:59:59.999")),

      SamplePeriod(1, "weekend",
        new LocalDateTime("2014-05-02T00:00:00.000"),
        new LocalDateTime("2014-05-02T23:59:59.999"))
    )

  val systems =
    periods.map { period =>
      (period, systemBuilder.systemBetween(period.start, period.end))
    }.toMap
  val observedSystems =
    periods.map { period =>
      (period, observedSystemBuilder.systemBetween(period.start, period.end, pruneStops=false))
    }.toMap
  val period = periods.head
  val system = systemBuilder.systemBetween(period.start, period.end)
  val observedSystem = observedSystemBuilder.systemBetween(period.start, period.end, pruneStops=false)

  // test the indicators
  // TODO: refactor indicator tests into separate classes with a trait that does most of the work

  def septaOverall(indicator: Indicator): AggregatedResults =
    PeriodResultAggregator({
      val results = periods.map { period => {
        val calculation = indicator.calculation(period)
        val transitSystem = systems(period)
        val results = calculation(transitSystem)
        (period, results)}
      }
      results.toMap}
    )


  def findRouteById(routes: Iterable[Route], id: String): Option[Route] =
    routes.find(_.id == id)

  def getResultByRouteId(byRoute: Map[Route, Double], id: String) = {
    findRouteById(byRoute.keys, id) match {
      case Some(r) => byRoute(r)
      case None => sys.error(s"Route $id isn't in the result set")
    }
  }
}

trait StopBuffersSpec {this: IndicatorSpec =>
  val stopBuffers = db withSession { implicit session =>
    StopBuffers(systems, 500, db)
  }
  trait StopBuffersSpecParams extends StopBuffers {
    def bufferForStop(stop: Stop): Projected[MultiPolygon] = stopBuffers.bufferForStop(stop)
    def bufferForStops(stops: Seq[Stop]): Projected[MultiPolygon] = stopBuffers.bufferForStops(stops)
    def bufferForPeriod(period: SamplePeriod): Projected[MultiPolygon] = stopBuffers.bufferForPeriod(period)
  }
}

trait RoadLengthSpec { this: IndicatorSpec =>
  val testRoadLength = db withSession { implicit session =>
    RoadLength.totalRoadLength
  }
  trait RoadLengthSpecParams extends RoadLength {
    def totalRoadLength = testRoadLength
  }
}




trait ObservedStopTimeSpec { this: IndicatorSpec =>
  lazy val observedTripMapping: Map[SamplePeriod, Map[String, Trip]] = {
    observedSystems.map { case (period, sys) =>
      period -> {
        sys.routes.map { route =>
          route.trips.map { trip =>
            (trip.id -> trip)
          }
        }
        .flatten
        .toMap
      }
    }
    .toMap
  }

  lazy val observedPeriodTrips: Map[String, Seq[(ScheduledStop, ScheduledStop)]] = {
    val scheduledTrips = system.routes.map(_.trips).flatten
    val observedTrips = observedTripMapping(period)
    scheduledTrips.map { trip =>
      (trip.id -> {
        val schedStops: Map[String, ScheduledStop] =
          trip.schedule.map(sst => sst.stop.id -> sst).toMap
        val obsvdStops: Map[String, ScheduledStop] =
          observedTrips(trip.id).schedule.map(ost => ost.stop.id -> ost).toMap
        for (s <- trip.schedule)
          yield (schedStops(s.stop.id), obsvdStops(s.stop.id))
      }) // Seq[(String, Seq[(ScheduledStop, ScheduledStop)])]
    }.toMap
  }

  trait ObservedStopTimeSpecParams extends ObservedStopTimes {
    def observedTripById(period: SamplePeriod): Map[String, Trip] =
      observedTripMapping(period)

    // Testing, so just return the same period every time.
    def observedStopsByTrip(period: SamplePeriod): Map[String, Seq[(ScheduledStop, ScheduledStop)]] =
      observedPeriodTrips
  }
}

trait BoundariesSpec {this: IndicatorSpec =>
  val testBoundary = db withSession { implicit session =>
    Boundaries.cityBoundary(1)
  }

  trait BoundariesSpecParams extends Boundaries {
    val cityBoundary = testBoundary
    val regionBoundary = testBoundary
  }
}

trait DemographicsSpec {this: IndicatorSpec =>
  val demographics = db withSession { implicit session =>
    Demographics(db)
  }

  trait DemographicsSpecParams extends Demographics {
    def populationMetricForBuffer(buffer: Projected[MultiPolygon], columnName: String) =
      demographics.populationMetricForBuffer(buffer, columnName)
  }
}

class IndicatorCalculatorSpec extends FlatSpec with Matchers with IndicatorSpec {
}
