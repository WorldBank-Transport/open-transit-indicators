package com.azavea.gtfs.io.csv

import com.azavea.gtfs._

import com.azavea.opentransit.testkit._

import org.scalatest._
import java.io.File

class CsvParserTest extends FunSuite with Matchers {
  test("CSV Parsing") {
    val data = CsvGtfsRecords(TestFiles.ashevillePath)
    data.stopTimeRecords.size should not be (0)
  }
}
