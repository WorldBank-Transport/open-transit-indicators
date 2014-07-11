package opentransitgt

import com.azavea.gtfs._
import com.azavea.gtfs.data._
import com.azavea.gtfs.slick._

import org.scalatest._

class IndicatorsCalculatorSpec extends FlatSpec with Matchers {
  // load SEPTA rail test data (has shapes.txt)
  val septaRailData = GtfsData.fromFile("src/test/resources/septa_data/")
  val septaRailCalc = new IndicatorsCalculator(septaRailData)

  it should "calculate numRoutesPerMode for SEPTA" in {
    septaRailCalc.numRoutesPerMode("Rail") should be (14)
  }

  it should "calculate maxStopsPerRoute for SEPTA" in {
    septaRailCalc.maxStopsPerRoute("AIR") should be (10)
    septaRailCalc.maxStopsPerRoute("CHE") should be (15)
    septaRailCalc.maxStopsPerRoute("CHW") should be (14)
    septaRailCalc.maxStopsPerRoute("CYN") should be (5)
    septaRailCalc.maxStopsPerRoute("FOX") should be (10)
    septaRailCalc.maxStopsPerRoute("GLN") should be (0)
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
    septaRailCalc.numStopsPerMode("Rail") should be (154)
  }

  it should "calculate avgTransitLengthPerMode for SEPTA" in {
    // TODO: this is in WGS84, but needs to be in UTM
    // Commenting this out until UTM is implemented, since there's no way to
    // tell if the current number is actually correct or not.
    //septaRailCalc.avgTransitLengthPerMode("Rail") should be (0.53766 plusOrMinus 1e-5)
  }

  // TODO: Asheville data fails to parse -- Eugene is looking into this.
  // Commenting out the Asheville section of tests for the time being.
  //
  // load Asheville test data (has shapes.txt)
  /*
  val ashevilleData = GtfsData.fromFile("src/test/resources/asheville_data/")
  val ashevilleCalc = new IndicatorsCalculator(ashevilleData)

  it should "calculate numRoutesPerMode for Asheville" in {
    ashevilleCalc.numRoutesPerMode("Bus") should be (16)
  }

  it should "calculate maxStopsPerRoute for Asheville" in {
    ashevilleCalc.maxStopsPerRoute("1127") should be (76)
    ashevilleCalc.maxStopsPerRoute("1128") should be (56)
    ashevilleCalc.maxStopsPerRoute("1129") should be (68)
    ashevilleCalc.maxStopsPerRoute("1130") should be (68)
    ashevilleCalc.maxStopsPerRoute("1131") should be (62)
    ashevilleCalc.maxStopsPerRoute("1132") should be (66)
    ashevilleCalc.maxStopsPerRoute("1133") should be (21)
    ashevilleCalc.maxStopsPerRoute("1134") should be (58)
    ashevilleCalc.maxStopsPerRoute("1135") should be (65)
    ashevilleCalc.maxStopsPerRoute("1136") should be (45)
    ashevilleCalc.maxStopsPerRoute("1137") should be (82)
    ashevilleCalc.maxStopsPerRoute("1138") should be (30)
    ashevilleCalc.maxStopsPerRoute("1139") should be (66)
    ashevilleCalc.maxStopsPerRoute("1140") should be (69)
    ashevilleCalc.maxStopsPerRoute("1141") should be (34)
    ashevilleCalc.maxStopsPerRoute("1142") should be (31)
  }

  it should "calculate numStopsPerMode for Asheville" in {
    ashevilleCalc.numStopsPerMode("Bus") should be (897)
  }

  it should "calculate avgTransitLengthPerMode for Asheville" in {
    // TODO: this is in WGS84, but needs to be in UTM
    // Commenting this out until UTM is implemented, since there's no way to
    // tell if the current number is actually correct or not.
    //ashevilleCalc.avgTransitLengthPerMode("Bus") should be (0.18362 plusOrMinus 1e-5)
  }

  // load GTFS sample test data (has empty shapes.txt)
  val sampleData = GtfsData.fromFile("src/test/resources/sample_feed/")
  val sampleCalc = new IndicatorsCalculator(sampleData)

  it should "calculate numRoutesPerMode for Sample" in {
    sampleCalc.numRoutesPerMode("Bus") should be (5)
  }

  it should "calculate maxStopsPerRoute for Sample" in {
    sampleCalc.maxStopsPerRoute("CITY") should be (5)
    sampleCalc.maxStopsPerRoute("BFC") should be (2)
    sampleCalc.maxStopsPerRoute("AB") should be (2)
    sampleCalc.maxStopsPerRoute("AAMV") should be (2)
    sampleCalc.maxStopsPerRoute("STBA") should be (2)
  }

  it should "calculate numStopsPerMode for Sample" in {
    sampleCalc.numStopsPerMode("Bus") should be (13)
  }

  it should "calculate avgTransitLengthPerMode for Sample" in {
    // has no shapes.txt, so the transit length should be 0
    sampleCalc.avgTransitLengthPerMode("Bus") should be (0.0)
  }
  */
}
