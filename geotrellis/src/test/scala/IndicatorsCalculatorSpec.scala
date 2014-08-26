package opentransitgt

import com.azavea.gtfs._
import com.azavea.gtfs.data._
import com.azavea.gtfs.slick._
import com.github.nscala_time.time.Imports._
import com.typesafe.config.{ConfigFactory,Config}
import opentransitgt.DjangoAdapter._
import org.scalatest._
import scala.slick.jdbc.JdbcBackend.{Database, Session}

class IndicatorsCalculatorSpec extends FlatSpec with PostgresSpec with Matchers {
  // initialize sample periods
  val calcParams = CalcParams("Token", 123, List[SamplePeriod](
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
  ))
  val period = calcParams.sample_periods.head
  val dao = new DAO

  // make the GeoTrellisService use the test database
  GeoTrellisService.db = db
  // load SEPTA rail test data into the database (has shapes.txt)
  GeoTrellisService.parseAndStore("src/test/resources/septa_data/")

  // set the geometry column name in order to retrieve in UTM
  dao.geomColumnName = config.getString("database.geom-name-utm")

  // read the reprojected data from the database
  var septaRailCalc: Option[IndicatorsCalculator] = None
  db withSession { implicit session: Session =>
    val septaRailData = dao.toGtfsData
    septaRailCalc = Some(new IndicatorsCalculator(septaRailData, calcParams))
  }

  // test the indicators
  // TODO: refactor indicator tests into separate classes with a trait that does most of the work
  septaRailCalc match {
    case None => fail()
    case Some(septaRailCalc) => {
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

      it should "calculate length by mode for SEPTA" in {
        val lengthByMode = septaRailCalc.calculatorsByName("length").calcByMode(period)
        lengthByMode(2) should be (50491.14587 plusOrMinus 1e-5)
      }

      it should "calculate overall length by mode for SEPTA" in {
        val lengthByMode = septaRailCalc.calculatorsByName("length").calcOverallByMode
        lengthByMode(2) should be (47523.43482 plusOrMinus 1e-5)
      }

      it should "calculate length by route for SEPTA" in {
        val lengthByRoute = septaRailCalc.calculatorsByName("length").calcByRoute(period)
        lengthByRoute("AIR") should be ( 21869.07041 plusOrMinus 1e-5)
        lengthByRoute("CHE") should be ( 22475.20005 plusOrMinus 1e-5)
        lengthByRoute("CHW") should be ( 23381.68619 plusOrMinus 1e-5)
        lengthByRoute("CYN") should be ( 10088.48478 plusOrMinus 1e-5)
        lengthByRoute("FOX") should be ( 14583.65208 plusOrMinus 1e-5)
        lengthByRoute("LAN") should be ( 59691.38456 plusOrMinus 1e-5)
        lengthByRoute("MED") should be ( 58339.78184 plusOrMinus 1e-5)
        lengthByRoute("NOR") should be ( 34500.16020 plusOrMinus 1e-5)
        lengthByRoute("PAO") should be ( 61050.67204 plusOrMinus 1e-5)
        lengthByRoute("TRE") should be (117481.24562 plusOrMinus 1e-5)
        lengthByRoute("WAR") should be ( 45306.01143 plusOrMinus 1e-5)
        lengthByRoute("WIL") should be (130047.07158 plusOrMinus 1e-5)
        lengthByRoute("WTR") should be ( 57570.47550 plusOrMinus 1e-5)
      }

      it should "calculate overall length by route for SEPTA" in {
        val lengthByRoute = septaRailCalc.calculatorsByName("length").calcOverallByRoute
        lengthByRoute("AIR") should be ( 21868.81730 plusOrMinus 1e-5)
        lengthByRoute("CHE") should be ( 18584.54364 plusOrMinus 1e-5)
        lengthByRoute("CHW") should be ( 19110.63894 plusOrMinus 1e-5)
        lengthByRoute("CYN") should be (  4053.40906 plusOrMinus 1e-5)
        lengthByRoute("FOX") should be ( 14583.48329 plusOrMinus 1e-5)
        lengthByRoute("GLN") should be (     0.00000 plusOrMinus 1e-5)
        lengthByRoute("LAN") should be ( 53766.02689 plusOrMinus 1e-5)
        lengthByRoute("MED") should be ( 35065.42606 plusOrMinus 1e-5)
        lengthByRoute("NOR") should be ( 34499.76089 plusOrMinus 1e-5)
        lengthByRoute("PAO") should be ( 59629.12721 plusOrMinus 1e-5)
        lengthByRoute("TRE") should be (103943.50170 plusOrMinus 1e-5)
        lengthByRoute("WAR") should be ( 41077.04152 plusOrMinus 1e-5)
        lengthByRoute("WIL") should be (130045.56640 plusOrMinus 1e-5)
        lengthByRoute("WTR") should be ( 53447.57621 plusOrMinus 1e-5)
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

      it should "calculate regularity_headways by mode for SEPTA" in {
        val rhByMode = septaRailCalc.calculatorsByName("regularity_headways").calcByMode(period)
        rhByMode(2) should be (0.25888 plusOrMinus 1e-5)
      }

      it should "calculate overall regularity_headways by mode for SEPTA" in {
        val rhByMode = septaRailCalc.calculatorsByName("regularity_headways").calcOverallByMode
        rhByMode(2) should be (0.34069 plusOrMinus 1e-5)
      }

      it should "calculate regularity_headways by route for SEPTA" in {
        val rhByRoute = septaRailCalc.calculatorsByName("regularity_headways").calcByRoute(period)
        rhByRoute("AIR") should be (0.30820 plusOrMinus 1e-5)
        rhByRoute("CHE") should be (0.44895 plusOrMinus 1e-5)
        rhByRoute("CHW") should be (0.38082 plusOrMinus 1e-5)
        rhByRoute("CYN") should be (0.66370 plusOrMinus 1e-5)
        rhByRoute("FOX") should be (0.46021 plusOrMinus 1e-5)
        rhByRoute("LAN") should be (0.32222 plusOrMinus 1e-5)
        rhByRoute("MED") should be (0.35719 plusOrMinus 1e-5)
        rhByRoute("NOR") should be (0.34982 plusOrMinus 1e-5)
        rhByRoute("PAO") should be (0.25363 plusOrMinus 1e-5)
        rhByRoute("TRE") should be (0.39818 plusOrMinus 1e-5)
        rhByRoute("WAR") should be (0.50407 plusOrMinus 1e-5)
        rhByRoute("WIL") should be (0.40664 plusOrMinus 1e-5)
        rhByRoute("WTR") should be (0.39992 plusOrMinus 1e-5)
      }

      it should "calculate overall regularity_headways by route for SEPTA" in {
        val rhByRoute = septaRailCalc.calculatorsByName("regularity_headways").calcOverallByRoute
        rhByRoute("AIR") should be (0.43618 plusOrMinus 1e-5)
        rhByRoute("CHE") should be (0.52217 plusOrMinus 1e-5)
        rhByRoute("CHW") should be (0.48045 plusOrMinus 1e-5)
        rhByRoute("CYN") should be (0.21974 plusOrMinus 1e-5)
        rhByRoute("FOX") should be (0.52466 plusOrMinus 1e-5)
        rhByRoute("GLN") should be (0.00000 plusOrMinus 1e-5)
        rhByRoute("LAN") should be (0.46935 plusOrMinus 1e-5)
        rhByRoute("MED") should be (0.48672 plusOrMinus 1e-5)
        rhByRoute("NOR") should be (0.48491 plusOrMinus 1e-5)
        rhByRoute("PAO") should be (0.38301 plusOrMinus 1e-5)
        rhByRoute("TRE") should be (0.47254 plusOrMinus 1e-5)
        rhByRoute("WAR") should be (0.61901 plusOrMinus 1e-5)
        rhByRoute("WIL") should be (0.51010 plusOrMinus 1e-5)
        rhByRoute("WTR") should be (0.48118 plusOrMinus 1e-5)
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

      // this doesn't test an indicator, but is an example for how to read data from the db
      it should "be able to read trips from the database" in {
        db withSession { implicit session: Session =>
          dao.toGtfsData.trips.size should be (1662)
        }
      }
    }
  }
}
