package com.azavea.opentransit.indicators

import com.azavea.gtfs._

import com.azavea.opentransit.testkit._

import com.github.nscala_time.time.Imports._
import com.typesafe.config.{ConfigFactory,Config}

import org.scalatest._

class LengthSpec extends FlatSpec with Matchers with IndicatorSpec {
  // TODO: the length indicator needs to be reworked. The test results
  // will change because of this. Commenting them out for now.
  /*
  it should "calculate length by mode for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = Length(system)

    byRouteType(Rail) should be (656.38489 +- 1e-5)
  }

  it should "calculate overall length by mode for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(Length)

    byRouteType(Rail) should be (589.67491 +- 1e-5)
  }

  it should "calculate length by route for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = Length(system)

    getResultByRouteId(byRoute, "AIR") should be ( 21.86907 +- 1e-5)
    getResultByRouteId(byRoute, "CHE") should be ( 22.47520 +- 1e-5)
    getResultByRouteId(byRoute, "CHW") should be ( 23.38168 +- 1e-5)
    getResultByRouteId(byRoute, "CYN") should be ( 10.08848 +- 1e-5)
    getResultByRouteId(byRoute, "FOX") should be ( 14.58365 +- 1e-5)
    getResultByRouteId(byRoute, "LAN") should be ( 59.69138 +- 1e-5)
    getResultByRouteId(byRoute, "MED") should be ( 58.33978 +- 1e-5)
    getResultByRouteId(byRoute, "NOR") should be ( 34.50016 +- 1e-5)
    getResultByRouteId(byRoute, "PAO") should be ( 61.05067 +- 1e-5)
    getResultByRouteId(byRoute, "TRE") should be (117.48124 +- 1e-5)
    getResultByRouteId(byRoute, "WAR") should be ( 45.30601 +- 1e-5)
    getResultByRouteId(byRoute, "WIL") should be (130.04707 +- 1e-5)
    getResultByRouteId(byRoute, "WTR") should be ( 57.57047 +- 1e-5)
  }

  it should "calculate overall length by route for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(Length)

    getResultByRouteId(byRoute, "AIR") should be ( 21.86881 +- 1e-5)
    getResultByRouteId(byRoute, "CHE") should be ( 18.58454 +- 1e-5)
    getResultByRouteId(byRoute, "CHW") should be ( 19.11063 +- 1e-5)
    getResultByRouteId(byRoute, "CYN") should be (  4.05340 +- 1e-5)
    getResultByRouteId(byRoute, "FOX") should be ( 14.58348 +- 1e-5)
    getResultByRouteId(byRoute, "GLN") should be (  0.00000 +- 1e-5)
    getResultByRouteId(byRoute, "LAN") should be ( 53.76602 +- 1e-5)
    getResultByRouteId(byRoute, "MED") should be ( 35.06542 +- 1e-5)
    getResultByRouteId(byRoute, "NOR") should be ( 34.49976 +- 1e-5)
    getResultByRouteId(byRoute, "PAO") should be ( 59.62912 +- 1e-5)
    getResultByRouteId(byRoute, "TRE") should be (103.94350 +- 1e-5)
    getResultByRouteId(byRoute, "WAR") should be ( 41.07704 +- 1e-5)
    getResultByRouteId(byRoute, "WIL") should be (130.04556 +- 1e-5)
    getResultByRouteId(byRoute, "WTR") should be ( 53.44757 +- 1e-5)
  }

  it should "calculate length by system for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = Length(system)
    bySystem.get should be (656.38489 +- 1e-5)
  }

  it should "calculate overall length by system for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(Length)
    bySystem.get should be (589.67491 +- 1e-5)
  }
  */
}
