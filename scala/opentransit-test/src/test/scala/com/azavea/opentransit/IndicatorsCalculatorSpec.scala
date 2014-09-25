package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.opentransit.io.GtfsIngest

import com.azavea.opentransit.testkit._

import com.github.nscala_time.time.Imports._
import com.typesafe.config.{ConfigFactory,Config}

import org.scalatest._

import scala.slick.jdbc.JdbcBackend.Session

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
      GtfsIngest(TestFiles.septaPath)
      GtfsRecords.fromDatabase(dbGeomNameUtm).force
    }
  }

  val systemBuilder = TransitSystemBuilder(records)

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

  val period = periods.head
  val system = systemBuilder.systemBetween(period.start, period.end)
  val systems = 
    periods.map { period =>
      (period, systemBuilder.systemBetween(period.start, period.end))
    }.toMap

  // test the indicators
  // TODO: refactor indicator tests into separate classes with a trait that does most of the work

  def septaOverall(indicator: TransitSystemCalculation): AggregatedResults =
    PeriodResultAggregator(
      periods.map { period =>
        (period, indicator(systems(period)))
      }.toMap
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

class IndicatorCalculatorSpec extends FlatSpec with Matchers with IndicatorSpec {
  it should "calculate time_traveled_stops by mode for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = TimeTraveledStops(system)
    byRouteType(Rail) should be (3.65945 +- 1e-5)
  }

  it should "calculate overall time_traveled_stops by mode for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(TimeTraveledStops)
    byRouteType(Rail) should be (3.46391 +- 1e-5)
  }

  it should "calculate time_traveled_stops by route for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = TimeTraveledStops(system)

    getResultByRouteId(byRoute, "AIR") should be (3.85344 +- 1e-5)
    getResultByRouteId(byRoute, "CHE") should be (2.98798 +- 1e-5)
    getResultByRouteId(byRoute, "CHW") should be (3.30278 +- 1e-5)
    getResultByRouteId(byRoute, "CYN") should be (4.79069 +- 1e-5)
    getResultByRouteId(byRoute, "FOX") should be (4.15789 +- 1e-5)
    getResultByRouteId(byRoute, "LAN") should be (3.74403 +- 1e-5)
    getResultByRouteId(byRoute, "MED") should be (3.11929 +- 1e-5)
    getResultByRouteId(byRoute, "NOR") should be (3.86250 +- 1e-5)
    getResultByRouteId(byRoute, "PAO") should be (3.35068 +- 1e-5)
    getResultByRouteId(byRoute, "TRE") should be (5.08303 +- 1e-5)
    getResultByRouteId(byRoute, "WAR") should be (3.74180 +- 1e-5)
    getResultByRouteId(byRoute, "WIL") should be (3.73809 +- 1e-5)
    getResultByRouteId(byRoute, "WTR") should be (3.52087 +- 1e-5)
  }

  it should "calculate overall time_traveled_stops by route for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(TimeTraveledStops)

    getResultByRouteId(byRoute, "AIR") should be (3.85039 +- 1e-5)
    getResultByRouteId(byRoute, "CHE") should be (2.81840 +- 1e-5)
    getResultByRouteId(byRoute, "CHW") should be (3.13225 +- 1e-5)
    getResultByRouteId(byRoute, "CYN") should be (1.95500 +- 1e-5)
    getResultByRouteId(byRoute, "FOX") should be (4.08901 +- 1e-5)
    getResultByRouteId(byRoute, "GLN") should be (0.00000 +- 1e-5)
    getResultByRouteId(byRoute, "LAN") should be (3.62624 +- 1e-5)
    getResultByRouteId(byRoute, "MED") should be (2.88720 +- 1e-5)
    getResultByRouteId(byRoute, "NOR") should be (3.64666 +- 1e-5)
    getResultByRouteId(byRoute, "PAO") should be (3.08767 +- 1e-5)
    getResultByRouteId(byRoute, "TRE") should be (4.81956 +- 1e-5)
    getResultByRouteId(byRoute, "WAR") should be (3.62163 +- 1e-5)
    getResultByRouteId(byRoute, "WIL") should be (3.22206 +- 1e-5)
    getResultByRouteId(byRoute, "WTR") should be (3.43258 +- 1e-5)
  }

  it should "calculate time_traveled_stops by system for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = TimeTraveledStops(system)
    bySystem.get should be (3.65945 +- 1e-5)
  }

  it should "calculate overall time_traveled_stops by system for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(TimeTraveledStops)
    bySystem.get should be (3.46391 +- 1e-5)
  }

  it should "calculate avg_service_freq by mode for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = AverageServiceFrequency(system)
    byRouteType(Rail) should be (0.25888 +- 1e-5)
  }

  it should "calculate overall avg_service_freq by mode for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(AverageServiceFrequency)
    byRouteType(Rail) should be (0.34069 +- 1e-5)
  }

  it should "calculate avg_service_freq by route for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = AverageServiceFrequency(system)

    getResultByRouteId(byRoute, "AIR") should be (0.30820 +- 1e-5)
    getResultByRouteId(byRoute, "CHE") should be (0.44895 +- 1e-5)
    getResultByRouteId(byRoute, "CHW") should be (0.38082 +- 1e-5)
    getResultByRouteId(byRoute, "CYN") should be (0.66370 +- 1e-5)
    getResultByRouteId(byRoute, "FOX") should be (0.46021 +- 1e-5)
    getResultByRouteId(byRoute, "LAN") should be (0.32222 +- 1e-5)
    getResultByRouteId(byRoute, "MED") should be (0.35719 +- 1e-5)
    getResultByRouteId(byRoute, "NOR") should be (0.34982 +- 1e-5)
    getResultByRouteId(byRoute, "PAO") should be (0.25363 +- 1e-5)
    getResultByRouteId(byRoute, "TRE") should be (0.39818 +- 1e-5)
    getResultByRouteId(byRoute, "WAR") should be (0.50407 +- 1e-5)
    getResultByRouteId(byRoute, "WIL") should be (0.40664 +- 1e-5)
    getResultByRouteId(byRoute, "WTR") should be (0.39992 +- 1e-5)
  }

  it should "calculate overall avg_service_freq by route for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(AverageServiceFrequency)

    getResultByRouteId(byRoute, "AIR") should be (0.43618 +- 1e-5)
    getResultByRouteId(byRoute, "CHE") should be (0.52217 +- 1e-5)
    getResultByRouteId(byRoute, "CHW") should be (0.48045 +- 1e-5)
    getResultByRouteId(byRoute, "CYN") should be (0.21974 +- 1e-5)
    getResultByRouteId(byRoute, "FOX") should be (0.52466 +- 1e-5)
    getResultByRouteId(byRoute, "GLN") should be (0.00000 +- 1e-5)
    getResultByRouteId(byRoute, "LAN") should be (0.46935 +- 1e-5)
    getResultByRouteId(byRoute, "MED") should be (0.48672 +- 1e-5)
    getResultByRouteId(byRoute, "NOR") should be (0.48491 +- 1e-5)
    getResultByRouteId(byRoute, "PAO") should be (0.38301 +- 1e-5)
    getResultByRouteId(byRoute, "TRE") should be (0.47254 +- 1e-5)
    getResultByRouteId(byRoute, "WAR") should be (0.61901 +- 1e-5)
    getResultByRouteId(byRoute, "WIL") should be (0.51010 +- 1e-5)
    getResultByRouteId(byRoute, "WTR") should be (0.48118 +- 1e-5)
  }

  it should "calculate avg_service_freq by system for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = AverageServiceFrequency(system)

    bySystem.get should be (0.36349 +- 1e-5)
  }

  it should "calculate overall avg_service_freq by system for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(AverageServiceFrequency)

    bySystem.get should be (0.47632 +- 1e-5)
  }

  it should "return map of Route ID's and their geometries" in {
    val SystemGeometries(byRoute, byRouteType, bySystem) = SystemGeometries(system)

    val l1 = byRoute(findRouteById(byRoute.keys, "AIR").get)
    l1.lines.size should be (1)
    l1.lines.head.points.length should be (160)


    val l2 = byRoute(findRouteById(byRoute.keys, "LAN").get)
    l2.lines.size should be (1)
    l2.lines.head.points.length should be (415)

    val l3 = byRoute(findRouteById(byRoute.keys, "TRE").get)
    l3.lines.size should be (1)
    l3.lines.head.points.length should be (805)
  }

  it should "return map of Route modes and their geometries" in {
    val SystemGeometries(byRoute, byRouteType, bySystem) = SystemGeometries(system)

    val l1 = byRouteType(Rail)
    l1.lines.size should be (13)
  }

  it should "calcuate overall distance_between_stops by route for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(DistanceStops)

    byRouteType(Rail) should be (2.37463 +- 1e-5)
  }

  it should "calcuate distance_between_stops by route for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(DistanceStops)

    getResultByRouteId(byRoute, "PAO") should be (1.87185 +- 1e-5)
    getResultByRouteId(byRoute, "MED") should be (1.50170 +- 1e-5)
    getResultByRouteId(byRoute, "WAR") should be (1.70507 +- 1e-5)
    getResultByRouteId(byRoute, "NOR") should be (1.75658 +- 1e-5)
    getResultByRouteId(byRoute, "LAN") should be (2.19893 +- 1e-5)
    getResultByRouteId(byRoute, "CYN") should be (1.01335 +- 1e-5)
    getResultByRouteId(byRoute, "WIL") should be (5.45808 +- 1e-5)
    getResultByRouteId(byRoute, "AIR") should be (1.97381 +- 1e-5)
    getResultByRouteId(byRoute, "CHW") should be (1.37672 +- 1e-5)
    getResultByRouteId(byRoute, "WTR") should be (2.30839 +- 1e-5)
    getResultByRouteId(byRoute, "FOX") should be (1.67826 +- 1e-5)
    getResultByRouteId(byRoute, "CHE") should be (1.04458 +- 1e-5)
    getResultByRouteId(byRoute, "TRE") should be (5.58536 +- 1e-5)
  }

  it should "calcuate overall distance_between_stops by system for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(DistanceStops)

    bySystem.get should be (2.35755 +- 1e-5)
  }
}
