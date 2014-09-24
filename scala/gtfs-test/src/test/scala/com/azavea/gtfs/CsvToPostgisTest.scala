package com.azavea.gtfs

import com.azavea.gtfs.op._
import com.azavea.gtfs.io.database.DatabaseRecordImport
import scala.slick.jdbc.JdbcBackend.Database
import com.github.nscala_time.time.Imports._

import com.azavea.opentransit.testkit._

import org.scalatest._

class CsvToPostgisTest extends FunSuite with DatabaseTestFixture with Matchers {
  println("Reading Asheville GTFS records from file...")
  val records =
    GtfsRecords.fromFiles(TestFiles.ashevillePath)
      .interpolateStopTimes

  println("Importing Asheville GTFS into PostGIS...")
  db withTransaction { implicit trans =>
    DatabaseRecordImport(records)
  }
  println("Checking consistency...")

  def check[T](f: GtfsRecords => Seq[T]): Unit =
    db withSession { implicit session =>
      val dbRecs = f(GtfsRecords.fromDatabase).toList
      val fileRecs = f(records).toList
      dbRecs.size should be (fileRecs.size)
      for( (dbRecord, record) <- dbRecs.zip(fileRecs)) {
        dbRecord should be (record)
      }
    }

  test("match agencies") {
    check(_.agencies.sortBy(_.id))
  }

  test("match stops") {
    check(_.stops.sortBy(_.id))
  }

  test("match routeRecords") {
    check(_.routeRecords.sortBy(_.id))
  }

  test("match tripRecords") {
    check(_.tripRecords.sortBy(_.id))
  }

  test("match stopTimeRecords") {
    check(_.stopTimeRecords.sortBy { st => (st.stopId, st.tripId, st.sequence) })
  }

  test("match calendarRecords") {
    check(_.calendarRecords.sortBy { c => (c.serviceId, c.start) })
  }

  test("match calendarDateRecords") {
    check(_.calendarDateRecords.sortBy { cd => (cd.serviceId, cd.date) })
  }

  test("match tripShapes") {
    check(_.tripShapes.sortBy { ts => ts.id })
  }

  test("match frequencyRecords") {
    check(_.frequencyRecords.sortBy { f => (f.tripId, LocalDate.now.toLocalDateTime(LocalTime.Midnight) + f.start) })
  }
}
