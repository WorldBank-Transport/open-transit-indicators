package com.azavea.gtfs.io.csv

import org.scalatest._

class AgencyFileSpec extends FunSpec with Matchers {
  describe("AgencyFile") {
    it("should throw an exception when there's more than one blank ID in the file") {
      an [GtfsFormatException] should be thrownBy {
        AgencyFile.parse("data/single-files/agency-with-duplicates.txt")
      }
    }
  }
}
