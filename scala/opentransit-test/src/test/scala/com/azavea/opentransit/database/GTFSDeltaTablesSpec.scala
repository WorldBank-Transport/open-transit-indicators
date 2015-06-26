package com.azavea.opentransit.database

import com.azavea.opentransit._
import com.azavea.gtfs._
import com.azavea.opentransit.testkit._
import com.azavea.opentransit.indicators.calculators._

import geotrellis.vector._
import geotrellis.slick._

import org.scalatest._
import scala.slick.jdbc.JdbcBackend.{Database, Session}

class GTFSDeltaTableSpec extends FlatSpec
                             with Matchers
                             with DatabaseTestFixture {

  it should "create the correct deltatype" in {
    DeltaType(1) should be (GTFSAddition)
    DeltaType(-1) should be (GTFSRemoval)
  }

  it should "save a trip delta" in {
    dbForName(dbName) withSession { implicit session: Session =>
      TripDeltaStore
        .addTripShape(TripShape("a trip", Projected(Line(Point(0,0), Point(1,1)), 4326)))
    }
  }

}

