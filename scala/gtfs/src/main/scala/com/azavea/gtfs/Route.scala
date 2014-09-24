package com.azavea.gtfs

import geotrellis.vector._

trait Route {
  def id: String
  def shortName: String
  def longName: String
  def description: Option[String]
  def url: Option[String]
  def color: Option[String]
  def textColor: Option[String]

  def routeType: RouteType
  def agency: Agency

  def trips: Seq[Trip]
}

object Route {
  def apply(record: RouteRecord, trips: Seq[Trip], agencies: String => Agency): Route = {
    val t = trips // Is there a way to reference this param without rename in the def of the anonymous instantiation?
    new Route {
      def id = record.id
      def shortName = record.shortName
      def longName = record.longName
      def description = record.description
      def url = record.url
      def color = record.color
      def textColor = record.textColor

      def routeType = record.routeType
      val agency = agencies(record.agencyId)

      def trips = t
    }
  }
}
