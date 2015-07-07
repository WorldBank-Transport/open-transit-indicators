package com.azavea.opentransit.indicators.stations

import com.azavea.opentransit.testkit._
import com.azavea.opentransit.database._
import com.azavea.opentransit.indicators.calculators._

import org.scalatest._
import org.scalatest.OptionValues._

class StationStatsSpec
    extends FlatSpec
       with Matchers
       with TestDatabase {

  class MockStationStore extends StationCSVStore {
    var value: Option[CSVJob] = None
    def get(jobId: Int) = value
    def set(csvJob: CSVJob): Unit = {
      value = Some(csvJob)
    }
  }

  it should "Produce an array of bytes equivalent to a CSV file" in {
    val ssCSV = StationStatsCSV(0.0, 0, 1, new MockStationStore)
    ssCSV.value should be ("stationID,stationName,proximalPop1,proximalPop2,proximalJobs,accessibleJobs\n")
    ssCSV.print("ZOG")
    ssCSV.value should be ("stationID,stationName,proximalPop1,proximalPop2,proximalJobs,accessibleJobs\nZOG")
  }

  it should "Implement a minimal DSL for writing StationStats to the byte array" in {
    val ssCSV = StationStatsCSV(0.0, 0, 1, new MockStationStore)
    import ssCSV.{StationStats => PathDependentType}
    val testSS = PathDependentType("beep", "boop", 1, 2, 3, 4)
    testSS.write()
    ssCSV.value should be ("stationID,stationName,proximalPop1,proximalPop2,proximalJobs,accessibleJobs\nbeep,boop,1,2,3,4\n")
  }
}
