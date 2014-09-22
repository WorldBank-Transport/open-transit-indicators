package com.azavea.gtfs.io.csv

import org.scalatest._
import java.io.File

class CsvParserTest extends FunSuite with Matchers {
  test("CSV Parsing") {
    val data = CsvGtfsRecords("data/asheville")
    data.stopTimeRecords.size should not be (0)
  }
}
