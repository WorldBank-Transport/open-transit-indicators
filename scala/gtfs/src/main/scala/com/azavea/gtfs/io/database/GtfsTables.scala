package com.azavea.gtfs.io.database

import scala.slick.jdbc.JdbcBackend.Session

trait GtfsTables
    extends AgenciesTable
       with StopsTable
       with RouteRecordsTable
       with TripRecordsTable
       with StopTimeRecordsTable
       with CalendarRecordsTable
       with CalendarDateRecordsTable
       with TripShapesTable
       with FrequencyRecordsTable { self: Profile => }

// Used to make StopsTable easily available
class DatabaseStops(implicit session: Session) extends StopsTable { self: Profile =>
  import profile.simple._
}
