package com.azavea.gtfs.io.csv

import com.azavea.gtfs._
import org.scalatest._

import com.azavea.opentransit.testkit._

class AgencyFileSpec extends FunSpec with Matchers {
  describe("AgencyFile") {
    it("should throw an exception when there's more than one blank ID in the file") {
      an [GtfsFormatException] should be thrownBy {
        AgencyFile.parse(s"${TestFiles.singleFilesPath}/agency-with-duplicates.txt")
      }
    }
  }
}
