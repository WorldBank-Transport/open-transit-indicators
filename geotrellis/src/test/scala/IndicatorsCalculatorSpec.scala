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
  // initialize a sample period
  val start = new DateTime("2010-01-01T00:00:00.000-05:00");
  val end = new DateTime("2010-01-01T08:00:00.000-05:00");
  val period = SamplePeriod(1, "morning", start, end)
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
    septaRailCalc = Some(new IndicatorsCalculator(septaRailData, period))
  }

  // test the indicators
  septaRailCalc match {
    case None => fail()
    case Some(septaRailCalc) => {
      it should "calculate numRoutesPerMode for SEPTA" in {
        septaRailCalc.numRoutesPerMode(2) should be (13)
      }

      it should "calculate maxStopsPerRoute for SEPTA" in {
        septaRailCalc.maxStopsPerRoute("AIR") should be (10)
        septaRailCalc.maxStopsPerRoute("CHE") should be (15)
        septaRailCalc.maxStopsPerRoute("CHW") should be (14)
        septaRailCalc.maxStopsPerRoute("CYN") should be (5)
        septaRailCalc.maxStopsPerRoute("FOX") should be (10)
        septaRailCalc.maxStopsPerRoute.contains("GLN") should be (false)
        septaRailCalc.maxStopsPerRoute("LAN") should be (26)
        septaRailCalc.maxStopsPerRoute("MED") should be (19)
        septaRailCalc.maxStopsPerRoute("NOR") should be (17)
        septaRailCalc.maxStopsPerRoute("PAO") should be (26)
        septaRailCalc.maxStopsPerRoute("TRE") should be (15)
        septaRailCalc.maxStopsPerRoute("WAR") should be (17)
        septaRailCalc.maxStopsPerRoute("WIL") should be (22)
        septaRailCalc.maxStopsPerRoute("WTR") should be (23)
      }

      it should "calculate numStopsPerMode for SEPTA" in {
        septaRailCalc.numStopsPerMode(2) should be (154)
      }

      it should "calculate avgTransitLengthPerMode for SEPTA" in {
        septaRailCalc.avgTransitLengthPerMode(2) should be (50491.14587 plusOrMinus 1e-5)
      }

      it should "calculate avgTimeBetweenStopsPerMode for SEPTA" in {
        septaRailCalc.avgTimeBetweenStopsPerMode(2) should be (3.65945 plusOrMinus 1e-5)
      }

      it should "calculate avgTimeBetweenStopsPerRoute for SEPTA" in {
        septaRailCalc.avgTimeBetweenStopsPerRoute("AIR") should be (3.85344 plusOrMinus 1e-5)
        septaRailCalc.avgTimeBetweenStopsPerRoute("CHE") should be (2.98798 plusOrMinus 1e-5)
        septaRailCalc.avgTimeBetweenStopsPerRoute("CHW") should be (3.30278 plusOrMinus 1e-5)
        septaRailCalc.avgTimeBetweenStopsPerRoute("CYN") should be (4.79069 plusOrMinus 1e-5)
        septaRailCalc.avgTimeBetweenStopsPerRoute("FOX") should be (4.15789 plusOrMinus 1e-5)
        septaRailCalc.avgTimeBetweenStopsPerRoute("LAN") should be (3.74403 plusOrMinus 1e-5)
        septaRailCalc.avgTimeBetweenStopsPerRoute("MED") should be (3.11929 plusOrMinus 1e-5)
        septaRailCalc.avgTimeBetweenStopsPerRoute("NOR") should be (3.86250 plusOrMinus 1e-5)
        septaRailCalc.avgTimeBetweenStopsPerRoute("PAO") should be (3.35068 plusOrMinus 1e-5)
        septaRailCalc.avgTimeBetweenStopsPerRoute("TRE") should be (5.08303 plusOrMinus 1e-5)
        septaRailCalc.avgTimeBetweenStopsPerRoute("WAR") should be (3.74180 plusOrMinus 1e-5)
        septaRailCalc.avgTimeBetweenStopsPerRoute("WIL") should be (3.73809 plusOrMinus 1e-5)
        septaRailCalc.avgTimeBetweenStopsPerRoute("WTR") should be (3.52087 plusOrMinus 1e-5)
      }

      it should "calculate headwayByMode for SEPTA" in {
        septaRailCalc.headwayByMode(2) should be (0.25888 plusOrMinus 1e-5)
      }

      it should "calculate headwayByRoute for SEPTA" in {
        septaRailCalc.headwayByRoute("AIR") should be (0.30820 plusOrMinus 1e-5)
        septaRailCalc.headwayByRoute("CHE") should be (0.44895 plusOrMinus 1e-5)
        septaRailCalc.headwayByRoute("CHW") should be (0.38082 plusOrMinus 1e-5)
        septaRailCalc.headwayByRoute("CYN") should be (0.66370 plusOrMinus 1e-5)
        septaRailCalc.headwayByRoute("FOX") should be (0.46021 plusOrMinus 1e-5)
        septaRailCalc.headwayByRoute("LAN") should be (0.32222 plusOrMinus 1e-5)
        septaRailCalc.headwayByRoute("MED") should be (0.35719 plusOrMinus 1e-5)
        septaRailCalc.headwayByRoute("NOR") should be (0.34982 plusOrMinus 1e-5)
        septaRailCalc.headwayByRoute("PAO") should be (0.25363 plusOrMinus 1e-5)
        septaRailCalc.headwayByRoute("TRE") should be (0.39818 plusOrMinus 1e-5)
        septaRailCalc.headwayByRoute("WAR") should be (0.50407 plusOrMinus 1e-5)
        septaRailCalc.headwayByRoute("WIL") should be (0.40664 plusOrMinus 1e-5)
        septaRailCalc.headwayByRoute("WTR") should be (0.39992 plusOrMinus 1e-5)
      }

      // this doesn't test an indicator, but is an example for how to read data from the db
      it should "be able to read trips from the database" in {
        db withSession { implicit session: Session =>
          dao.toGtfsData.trips.size should be (1622)
        }
      }
    }
  }
}
