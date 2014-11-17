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
  def agency: Option[Agency]

  def trips: Seq[Trip]

  // override equals and hashCode so route id is used for comparison
  override def equals(o: Any) = o match {
    case that: Route => that.id.equalsIgnoreCase(this.id)
    case _ => false
  }

  override def hashCode = id.hashCode
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
      def agency = record.agencyId.map(agencies(_))

      def trips = t
    }
  }
}
