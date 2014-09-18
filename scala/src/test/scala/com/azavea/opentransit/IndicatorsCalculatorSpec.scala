package com.azavea.opentransit

import com.azavea.opentransit.DjangoAdapter._

import com.azavea.gtfs._
import com.azavea.gtfs.data._
import com.azavea.gtfs.slick._

import com.github.nscala_time.time.Imports._
import com.typesafe.config.{ConfigFactory,Config}

import org.scalatest._
import scala.slick.jdbc.JdbcBackend.{Database, Session}

class IndicatorsCalculatorSpec extends FlatSpec with PostgresSpec with Matchers {
  val config = ConfigFactory.load
  val geomColumnName = config.getString("database.geom-name-utm")

  // initialize sample periods
  val calcParams = CalcParams("Token", "cbe1f916-42d3-4630-8466-68b753024767",
    20000,  // poverty line ($)
    500,    // nearby buffer distance (m)
    3600,   // max commute time (s)
    1800,   // max walk time (s)
    1,      // city boundary id
    2,      // region boundary id
    2,      // average fare ($)
    List[SamplePeriod](
      SamplePeriod(1, "night",
        new DateTime("2010-01-01T00:00:00.000-05:00"),
        new DateTime("2010-01-01T08:00:00.000-05:00")),

      SamplePeriod(1, "morning",
        new DateTime("2010-01-01T08:00:00.000-05:00"),
        new DateTime("2010-01-01T11:00:00.000-05:00")),

      SamplePeriod(1, "midday",
        new DateTime("2010-01-01T11:00:00.000-05:00"),
        new DateTime("2010-01-01T16:30:00.000-05:00")),

      SamplePeriod(1, "evening",
        new DateTime("2010-01-01T16:30:00.000-05:00"),
        new DateTime("2010-01-01T23:59:59.999-05:00")),

      SamplePeriod(1, "weekend",
        new DateTime("2010-01-02T00:00:00.000-05:00"),
        new DateTime("2010-01-02T23:59:59.999-05:00"))
    )
  )

  val period = calcParams.sample_periods.head
  val dao = new DAO

  // make the GeoTrellisService use the test database
  trait ThisDatabase extends GtfsDatabase { val db = IndicatorsCalculatorSpec.this.db }
  val service = new GeoTrellisService with ThisDatabase

  // load SEPTA rail test data into the database (has shapes.txt)
  service.parseAndStore("src/test/resources/septa_data/")

  // set the geometry column name in order to retrieve in UTM
  dao.geomColumnName = geomColumnName

  // read the reprojected data from the database
  val septaRailCalc: IndicatorsCalculator = {
    db withSession { implicit session: Session =>
      val septaRailData = dao.toGtfsData
      new IndicatorsCalculator(septaRailData, calcParams, db)
    }
  }

  // test the indicators
  // TODO: refactor indicator tests into separate classes with a trait that does most of the work
  it should "calculate num_stops by mode for SEPTA" in {
    val numStopsByMode = septaRailCalc.calculatorsByName("num_stops").calcByMode(period)
    numStopsByMode(2) should be (154)
  }

  it should "calculate overall num_stops by mode for SEPTA" in {
    val numStopsByMode = septaRailCalc.calculatorsByName("num_stops").calcOverallByMode
    numStopsByMode(2) should be (150.72149 plusOrMinus 1e-5)
  }

  it should "calculate num_stops by route for SEPTA" in {
    val numStopsByRoute = septaRailCalc.calculatorsByName("num_stops").calcByRoute(period)
    numStopsByRoute("AIR") should be (10)
    numStopsByRoute("CHE") should be (15)
    numStopsByRoute("CHW") should be (14)
    numStopsByRoute("CYN") should be (5)
    numStopsByRoute("FOX") should be (10)
    numStopsByRoute.contains("GLN") should be (false)
    numStopsByRoute("LAN") should be (26)
    numStopsByRoute("MED") should be (19)
    numStopsByRoute("NOR") should be (17)
    numStopsByRoute("PAO") should be (26)
    numStopsByRoute("TRE") should be (15)
    numStopsByRoute("WAR") should be (17)
    numStopsByRoute("WIL") should be (22)
    numStopsByRoute("WTR") should be (23)
  }

  it should "calculate overall num_stops by route for SEPTA" in {
    val numStopsByRoute = septaRailCalc.calculatorsByName("num_stops").calcOverallByRoute
    numStopsByRoute("AIR") should be ( 9.99988 plusOrMinus 1e-5)
    numStopsByRoute("CHE") should be (14.68733 plusOrMinus 1e-5)
    numStopsByRoute("CHW") should be (13.99983 plusOrMinus 1e-5)
    numStopsByRoute("CYN") should be ( 2.00892 plusOrMinus 1e-5)
    numStopsByRoute("FOX") should be ( 9.71417 plusOrMinus 1e-5)
    numStopsByRoute("GLN") should be ( 0.00000 plusOrMinus 1e-5)
    numStopsByRoute("LAN") should be (24.68423 plusOrMinus 1e-5)
    numStopsByRoute("MED") should be (18.99978 plusOrMinus 1e-5)
    numStopsByRoute("NOR") should be (16.99980 plusOrMinus 1e-5)
    numStopsByRoute("PAO") should be (25.99969 plusOrMinus 1e-5)
    numStopsByRoute("TRE") should be (14.77662 plusOrMinus 1e-5)
    numStopsByRoute("WAR") should be (16.99980 plusOrMinus 1e-5)
    numStopsByRoute("WIL") should be (20.07121 plusOrMinus 1e-5)
    numStopsByRoute("WTR") should be (22.38963 plusOrMinus 1e-5)
  }

  it should "calculate num_stops by system for SEPTA" in {
    val numStopsBySystem = septaRailCalc.calculatorsByName("num_stops").calcBySystem(period)
    numStopsBySystem should be (154)
  }

  it should "calculate overall num_stops by system for SEPTA" in {
    val numStopsBySystem = septaRailCalc.calculatorsByName("num_stops").calcOverallBySystem
    numStopsBySystem should be (150.72149 plusOrMinus 1e-5)
  }

  it should "calculate num_routes by mode for SEPTA" in {
    val numRoutesByMode = septaRailCalc.calculatorsByName("num_routes").calcByMode(period)
    numRoutesByMode(2) should be (13)
  }

  it should "calculate overall num_routes by mode for SEPTA" in {
    val numRoutesByMode = septaRailCalc.calculatorsByName("num_routes").calcOverallByMode
    numRoutesByMode(2) should be (12.40164 plusOrMinus 1e-5)
  }

  it should "calculate num_routes by route for SEPTA" in {
    val numRoutesByRoute = septaRailCalc.calculatorsByName("num_routes").calcByRoute(period)
    numRoutesByRoute("AIR") should be (1)
    numRoutesByRoute("CHE") should be (1)
    numRoutesByRoute("CHW") should be (1)
    numRoutesByRoute("CYN") should be (1)
    numRoutesByRoute("FOX") should be (1)
    numRoutesByRoute("LAN") should be (1)
    numRoutesByRoute("MED") should be (1)
    numRoutesByRoute("NOR") should be (1)
    numRoutesByRoute("PAO") should be (1)
    numRoutesByRoute("TRE") should be (1)
    numRoutesByRoute("WAR") should be (1)
    numRoutesByRoute("WIL") should be (1)
    numRoutesByRoute("WTR") should be (1)
  }

  it should "calculate overall num_routes by route for SEPTA" in {
    val numRoutesByRoute = septaRailCalc.calculatorsByName("num_routes").calcOverallByRoute
    numRoutesByRoute("AIR") should be (1.0000 plusOrMinus 1e-4)
    numRoutesByRoute("CHE") should be (1.0000 plusOrMinus 1e-4)
    numRoutesByRoute("CHW") should be (1.0000 plusOrMinus 1e-4)
    numRoutesByRoute("CYN") should be (0.4017 plusOrMinus 1e-4)
    numRoutesByRoute("FOX") should be (1.0000 plusOrMinus 1e-4)
    numRoutesByRoute("GLN") should be (0.0000 plusOrMinus 1e-4)
    numRoutesByRoute("LAN") should be (1.0000 plusOrMinus 1e-4)
    numRoutesByRoute("MED") should be (1.0000 plusOrMinus 1e-4)
    numRoutesByRoute("NOR") should be (1.0000 plusOrMinus 1e-4)
    numRoutesByRoute("PAO") should be (1.0000 plusOrMinus 1e-4)
    numRoutesByRoute("TRE") should be (1.0000 plusOrMinus 1e-4)
    numRoutesByRoute("WAR") should be (1.0000 plusOrMinus 1e-4)
    numRoutesByRoute("WIL") should be (1.0000 plusOrMinus 1e-4)
    numRoutesByRoute("WTR") should be (1.0000 plusOrMinus 1e-4)
  }

  it should "calculate num_routes by system for SEPTA" in {
    val numRoutesBySystem = septaRailCalc.calculatorsByName("num_routes").calcBySystem(period)
    numRoutesBySystem should be (13)
  }

  it should "calculate overall num_routes by system for SEPTA" in {
    val numRoutesBySystem = septaRailCalc.calculatorsByName("num_routes").calcOverallBySystem
    numRoutesBySystem should be (12.40164 plusOrMinus 1e-5)
  }

  it should "calculate length by mode for SEPTA" in {
    val lengthByMode = septaRailCalc.calculatorsByName("length").calcByMode(period)
    lengthByMode(2) should be (656.38489 plusOrMinus 1e-5)
  }

  it should "calculate overall length by mode for SEPTA" in {
    val lengthByMode = septaRailCalc.calculatorsByName("length").calcOverallByMode
    lengthByMode(2) should be (589.67491 plusOrMinus 1e-5)
  }

  it should "calculate length by route for SEPTA" in {
    val lengthByRoute = septaRailCalc.calculatorsByName("length").calcByRoute(period)
    lengthByRoute("AIR") should be ( 21.86907 plusOrMinus 1e-5)
    lengthByRoute("CHE") should be ( 22.47520 plusOrMinus 1e-5)
    lengthByRoute("CHW") should be ( 23.38168 plusOrMinus 1e-5)
    lengthByRoute("CYN") should be ( 10.08848 plusOrMinus 1e-5)
    lengthByRoute("FOX") should be ( 14.58365 plusOrMinus 1e-5)
    lengthByRoute("LAN") should be ( 59.69138 plusOrMinus 1e-5)
    lengthByRoute("MED") should be ( 58.33978 plusOrMinus 1e-5)
    lengthByRoute("NOR") should be ( 34.50016 plusOrMinus 1e-5)
    lengthByRoute("PAO") should be ( 61.05067 plusOrMinus 1e-5)
    lengthByRoute("TRE") should be (117.48124 plusOrMinus 1e-5)
    lengthByRoute("WAR") should be ( 45.30601 plusOrMinus 1e-5)
    lengthByRoute("WIL") should be (130.04707 plusOrMinus 1e-5)
    lengthByRoute("WTR") should be ( 57.57047 plusOrMinus 1e-5)
  }

  it should "calculate overall length by route for SEPTA" in {
    val lengthByRoute = septaRailCalc.calculatorsByName("length").calcOverallByRoute
    lengthByRoute("AIR") should be ( 21.86881 plusOrMinus 1e-5)
    lengthByRoute("CHE") should be ( 18.58454 plusOrMinus 1e-5)
    lengthByRoute("CHW") should be ( 19.11063 plusOrMinus 1e-5)
    lengthByRoute("CYN") should be (  4.05340 plusOrMinus 1e-5)
    lengthByRoute("FOX") should be ( 14.58348 plusOrMinus 1e-5)
    lengthByRoute("GLN") should be (  0.00000 plusOrMinus 1e-5)
    lengthByRoute("LAN") should be ( 53.76602 plusOrMinus 1e-5)
    lengthByRoute("MED") should be ( 35.06542 plusOrMinus 1e-5)
    lengthByRoute("NOR") should be ( 34.49976 plusOrMinus 1e-5)
    lengthByRoute("PAO") should be ( 59.62912 plusOrMinus 1e-5)
    lengthByRoute("TRE") should be (103.94350 plusOrMinus 1e-5)
    lengthByRoute("WAR") should be ( 41.07704 plusOrMinus 1e-5)
    lengthByRoute("WIL") should be (130.04556 plusOrMinus 1e-5)
    lengthByRoute("WTR") should be ( 53.44757 plusOrMinus 1e-5)
  }

  it should "calculate length by system for SEPTA" in {
    val lengthBySystem = septaRailCalc.calculatorsByName("length").calcBySystem(period)
    lengthBySystem should be (656.38489 plusOrMinus 1e-5)
  }

  it should "calculate overall length by system for SEPTA" in {
    val lengthBySystem = septaRailCalc.calculatorsByName("length").calcOverallBySystem
    lengthBySystem should be (589.67491 plusOrMinus 1e-5)
  }

  it should "calculate time_traveled_stops by mode for SEPTA" in {
    val ttsByMode = septaRailCalc.calculatorsByName("time_traveled_stops").calcByMode(period)
    ttsByMode(2) should be (3.65945 plusOrMinus 1e-5)
  }

  it should "calculate overall time_traveled_stops by mode for SEPTA" in {
    val ttsByMode = septaRailCalc.calculatorsByName("time_traveled_stops").calcOverallByMode
    ttsByMode(2) should be (3.46391 plusOrMinus 1e-5)
  }

  it should "calculate time_traveled_stops by route for SEPTA" in {
    val ttsByRoute = septaRailCalc.calculatorsByName("time_traveled_stops").calcByRoute(period)
    ttsByRoute("AIR") should be (3.85344 plusOrMinus 1e-5)
    ttsByRoute("CHE") should be (2.98798 plusOrMinus 1e-5)
    ttsByRoute("CHW") should be (3.30278 plusOrMinus 1e-5)
    ttsByRoute("CYN") should be (4.79069 plusOrMinus 1e-5)
    ttsByRoute("FOX") should be (4.15789 plusOrMinus 1e-5)
    ttsByRoute("LAN") should be (3.74403 plusOrMinus 1e-5)
    ttsByRoute("MED") should be (3.11929 plusOrMinus 1e-5)
    ttsByRoute("NOR") should be (3.86250 plusOrMinus 1e-5)
    ttsByRoute("PAO") should be (3.35068 plusOrMinus 1e-5)
    ttsByRoute("TRE") should be (5.08303 plusOrMinus 1e-5)
    ttsByRoute("WAR") should be (3.74180 plusOrMinus 1e-5)
    ttsByRoute("WIL") should be (3.73809 plusOrMinus 1e-5)
    ttsByRoute("WTR") should be (3.52087 plusOrMinus 1e-5)
  }

  it should "calculate overall time_traveled_stops by route for SEPTA" in {
    val ttsByRoute = septaRailCalc.calculatorsByName("time_traveled_stops").calcOverallByRoute
    ttsByRoute("AIR") should be (3.85039 plusOrMinus 1e-5)
    ttsByRoute("CHE") should be (2.81840 plusOrMinus 1e-5)
    ttsByRoute("CHW") should be (3.13225 plusOrMinus 1e-5)
    ttsByRoute("CYN") should be (1.95500 plusOrMinus 1e-5)
    ttsByRoute("FOX") should be (4.08901 plusOrMinus 1e-5)
    ttsByRoute("GLN") should be (0.00000 plusOrMinus 1e-5)
    ttsByRoute("LAN") should be (3.62624 plusOrMinus 1e-5)
    ttsByRoute("MED") should be (2.88720 plusOrMinus 1e-5)
    ttsByRoute("NOR") should be (3.64666 plusOrMinus 1e-5)
    ttsByRoute("PAO") should be (3.08767 plusOrMinus 1e-5)
    ttsByRoute("TRE") should be (4.81956 plusOrMinus 1e-5)
    ttsByRoute("WAR") should be (3.62163 plusOrMinus 1e-5)
    ttsByRoute("WIL") should be (3.22206 plusOrMinus 1e-5)
    ttsByRoute("WTR") should be (3.43258 plusOrMinus 1e-5)
  }

  it should "calculate time_traveled_stops by system for SEPTA" in {
    val ttsBySystem = septaRailCalc.calculatorsByName("time_traveled_stops").calcBySystem(period)
    ttsBySystem should be (3.65945 plusOrMinus 1e-5)
  }

  it should "calculate overall time_traveled_stops by system for SEPTA" in {
    val ttsBySystem = septaRailCalc.calculatorsByName("time_traveled_stops").calcOverallBySystem
    ttsBySystem should be (3.46391 plusOrMinus 1e-5)
  }

  it should "calculate avg_service_freq by mode for SEPTA" in {
    val asfByMode = septaRailCalc.calculatorsByName("avg_service_freq").calcByMode(period)
    asfByMode(2) should be (0.25888 plusOrMinus 1e-5)
  }

  it should "calculate overall avg_service_freq by mode for SEPTA" in {
    val asfByMode = septaRailCalc.calculatorsByName("avg_service_freq").calcOverallByMode
    asfByMode(2) should be (0.34069 plusOrMinus 1e-5)
  }

  it should "calculate avg_service_freq by route for SEPTA" in {
    val asfByRoute = septaRailCalc.calculatorsByName("avg_service_freq").calcByRoute(period)
    asfByRoute("AIR") should be (0.30820 plusOrMinus 1e-5)
    asfByRoute("CHE") should be (0.44895 plusOrMinus 1e-5)
    asfByRoute("CHW") should be (0.38082 plusOrMinus 1e-5)
    asfByRoute("CYN") should be (0.66370 plusOrMinus 1e-5)
    asfByRoute("FOX") should be (0.46021 plusOrMinus 1e-5)
    asfByRoute("LAN") should be (0.32222 plusOrMinus 1e-5)
    asfByRoute("MED") should be (0.35719 plusOrMinus 1e-5)
    asfByRoute("NOR") should be (0.34982 plusOrMinus 1e-5)
    asfByRoute("PAO") should be (0.25363 plusOrMinus 1e-5)
    asfByRoute("TRE") should be (0.39818 plusOrMinus 1e-5)
    asfByRoute("WAR") should be (0.50407 plusOrMinus 1e-5)
    asfByRoute("WIL") should be (0.40664 plusOrMinus 1e-5)
    asfByRoute("WTR") should be (0.39992 plusOrMinus 1e-5)
  }

  it should "calculate overall avg_service_freq by route for SEPTA" in {
    val asfByRoute = septaRailCalc.calculatorsByName("avg_service_freq").calcOverallByRoute
    asfByRoute("AIR") should be (0.43618 plusOrMinus 1e-5)
    asfByRoute("CHE") should be (0.52217 plusOrMinus 1e-5)
    asfByRoute("CHW") should be (0.48045 plusOrMinus 1e-5)
    asfByRoute("CYN") should be (0.21974 plusOrMinus 1e-5)
    asfByRoute("FOX") should be (0.52466 plusOrMinus 1e-5)
    asfByRoute("GLN") should be (0.00000 plusOrMinus 1e-5)
    asfByRoute("LAN") should be (0.46935 plusOrMinus 1e-5)
    asfByRoute("MED") should be (0.48672 plusOrMinus 1e-5)
    asfByRoute("NOR") should be (0.48491 plusOrMinus 1e-5)
    asfByRoute("PAO") should be (0.38301 plusOrMinus 1e-5)
    asfByRoute("TRE") should be (0.47254 plusOrMinus 1e-5)
    asfByRoute("WAR") should be (0.61901 plusOrMinus 1e-5)
    asfByRoute("WIL") should be (0.51010 plusOrMinus 1e-5)
    asfByRoute("WTR") should be (0.48118 plusOrMinus 1e-5)
  }

  it should "calculate avg_service_freq by system for SEPTA" in {
    val asfBySystem = septaRailCalc.calculatorsByName("avg_service_freq").calcBySystem(period)
    asfBySystem should be (0.36349 plusOrMinus 1e-5)
  }

  it should "calculate overall avg_service_freq by system for SEPTA" in {
    val asfBySystem = septaRailCalc.calculatorsByName("avg_service_freq").calcOverallBySystem
    asfBySystem should be (0.47632 plusOrMinus 1e-5)
  }

  it should "return map of Route ID's and their geometries" in {
    val lengthCalc = septaRailCalc.calculatorsByName("length")
    lengthCalc.lineForRouteIDLatLng(period)("AIR") match {
      case None => fail
      case Some(shapeLine) => shapeLine.points.length should be (160)
    }
    lengthCalc.lineForRouteIDLatLng(period)("LAN") match {
      case None => fail
      case Some(shapeLine) => shapeLine.points.length should be (415)
    }
    lengthCalc.lineForRouteIDLatLng(period)("TRE") match {
      case None => fail
      case Some(shapeLine) => shapeLine.points.length should be (805)
    }
  }

  it should "return map of Route modes and their geometries" in {
    val lengthCalc = septaRailCalc.calculatorsByName("length")

    lengthCalc.multiLineForRouteModeLatLng(period)(2) match {
      case None => fail
      case Some(shapeLine) => shapeLine.lines(0).points.length should be (160) // this is the "AIR" line above
    }
    lengthCalc.multiLineForRouteModeLatLng(period)(2) match {
      case None => fail
      case Some(shapeLine) => shapeLine.lines.length should be (13) // there are 13 lines for this mode
    }
  }

  it should "calcuate overall distance_between_stops by route for SEPTA" in {
    val dbsByMode = septaRailCalc.calculatorsByName("distance_stops").calcOverallByMode
    dbsByMode(2) should be (2.37463 plusOrMinus 1e-5)
  }

  it should "calcuate distance_between_stops by route for SEPTA" in {
    val dbsByRoute = septaRailCalc.calculatorsByName("distance_stops").calcOverallByRoute
    dbsByRoute("PAO") should be (1.87185 plusOrMinus 1e-5)
    dbsByRoute("MED") should be (1.50170 plusOrMinus 1e-5)
    dbsByRoute("WAR") should be (1.70507 plusOrMinus 1e-5)
    dbsByRoute("NOR") should be (1.75658 plusOrMinus 1e-5)
    dbsByRoute("LAN") should be (2.19893 plusOrMinus 1e-5)
    dbsByRoute("CYN") should be (1.01335 plusOrMinus 1e-5)
    dbsByRoute("WIL") should be (5.45808 plusOrMinus 1e-5)
    dbsByRoute("AIR") should be (1.97381 plusOrMinus 1e-5)
    dbsByRoute("CHW") should be (1.37672 plusOrMinus 1e-5)
    dbsByRoute("WTR") should be (2.30839 plusOrMinus 1e-5)
    dbsByRoute("FOX") should be (1.67826 plusOrMinus 1e-5)
    dbsByRoute("CHE") should be (1.04458 plusOrMinus 1e-5)
    dbsByRoute("TRE") should be (5.58536 plusOrMinus 1e-5)
  }

  it should "calcuate overall distance_between_stops by system for SEPTA" in {
    val dbsBySystem = septaRailCalc.calculatorsByName("distance_stops").calcOverallBySystem
    dbsBySystem should be (2.35755 plusOrMinus 1e-5)
  }

  // this doesn't test an indicator, but is an example for how to read data from the db
  it should "be able to read trips from the database" in {
    db withSession { implicit session: Session =>
      dao.toGtfsData.trips.size should be (1662)
    }
  }
}
