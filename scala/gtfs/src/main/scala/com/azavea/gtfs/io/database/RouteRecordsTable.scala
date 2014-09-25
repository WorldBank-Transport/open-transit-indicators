package com.azavea.gtfs.io.database

import com.azavea.gtfs._

trait RouteRecordsTable { this: Profile =>
  import profile.simple._

  class RouteRecords(tag: Tag) extends Table[RouteRecord](tag, "gtfs_routes") {
    def id = column[String]("route_id", O.PrimaryKey)
    def short_name = column[String]("route_short_name")
    def long_name = column[String]("route_long_name")
    def route_type = column[RouteType]("route_type")
    def agency_id = column[String]("agency_id")
    def route_desc = column[Option[String]]("route_desc")
    def route_url = column[Option[String]]("route_url")
    def route_color = column[Option[String]]("route_color")
    def route_text_color = column[Option[String]]("route_text_color")

    def * = (id, short_name, long_name, route_type, agency_id, route_desc, route_url, route_color, route_text_color)  <>
      (RouteRecord.tupled, RouteRecord.unapply)
  }

  val routeRecordsTable = TableQuery[RouteRecords]
}
