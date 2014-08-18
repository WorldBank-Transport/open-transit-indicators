package opentransitgt

import com.azavea.gtfs._
import com.azavea.gtfs.data._
import com.azavea.gtfs.slick._
import com.github.nscala_time.time.Imports._
import opentransitgt.DjangoAdapter._
import org.scalatest._

class IndicatorsCalculatorSpec extends FlatSpec with Matchers {
  // initialize a sample period
  val start = new DateTime("2010-01-01T00:00:00.000-05:00");
  val end = new DateTime("2010-01-01T08:00:00.000-05:00");
  val period = SamplePeriod(1, "morning", start, end)

  // load SEPTA rail test data (has shapes.txt)
  val septaRailData = GtfsData.fromFile("src/test/resources/septa_data/")
  val septaRailCalc = new IndicatorsCalculator(septaRailData, period)

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
    // TODO: this is in WGS84, but needs to be in UTM
    // Commenting this out until UTM is implemented, since there's no way to
    // tell if the current number is actually correct or not.
    //septaRailCalc.avgTransitLengthPerMode("Rail") should be (0.53766 plusOrMinus 1e-5)
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
    septaRailCalc.headwayByRoute("CYN") should be (0.66370  plusOrMinus 1e-5)
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
