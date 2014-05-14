package opentransitgt

import com.azavea.gtfs._
import com.azavea.gtfs.data._
import com.azavea.gtfs.slick._

import org.scalatest._

class IndicatorsCalculatorSpec extends FlatSpec with Matchers {
  // load SEPTA rail test data
  val septaRailData = GtfsData.fromFile("src/test/resources/septa_data/")
  val septaRailCalc = new IndicatorsCalculator(septaRailData)

  it should "calculate numRoutesPerMode" in {
    septaRailCalc.numRoutesPerMode("Rail") should be (14)
  }

  it should "calculate maxStopsPerRoute" in {
    septaRailCalc.maxStopsPerRoute("TRE") should be (15)
    septaRailCalc.maxStopsPerRoute("CHE") should be (15)
    septaRailCalc.maxStopsPerRoute("PAO") should be (26)
    septaRailCalc.maxStopsPerRoute("WAR") should be (17)
    septaRailCalc.maxStopsPerRoute("LAN") should be (26)
    septaRailCalc.maxStopsPerRoute("CHW") should be (14)
    septaRailCalc.maxStopsPerRoute("CYN") should be (5)
    septaRailCalc.maxStopsPerRoute("WTR") should be (23)
    septaRailCalc.maxStopsPerRoute("NOR") should be (17)
    septaRailCalc.maxStopsPerRoute("FOX") should be (10)
    septaRailCalc.maxStopsPerRoute("MED") should be (19)
    septaRailCalc.maxStopsPerRoute("AIR") should be (10)
    septaRailCalc.maxStopsPerRoute("WIL") should be (22)
    septaRailCalc.maxStopsPerRoute("GLN") should be (0)
  }

  it should "calculate numStopsPerMode" in {
    septaRailCalc.numStopsPerMode("Rail") should be (219)
  }

  it should "calculate avgTransitLengthPerMode" in {
    // SEPTA rail GTFS doesn't have transit lengths defined
    septaRailCalc.avgTransitLengthPerMode("Rail") should be (0.0)
  }
}
