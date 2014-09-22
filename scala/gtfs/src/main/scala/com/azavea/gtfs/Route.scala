package com.azavea.gtfs

// case class Route(
//   record: RouteRecord,
//   trips: Seq[Trip]
// )

trait Route {
  def shortName: String
  def longName: String
  def description: Option[String]
  def url: Option[String]
  def color: Option[String]
  def textColor: Option[String]

  def routeType: RouteType
  def agency: Option[Agency]

  def trips: Seq[Trip]
}

object Route {
  def apply(record: RouteRecord, trips: Seq[Trip], agencies: String => Agency): Route = {
    val t = trips // Is there a way to reference this param without rename in the def of the anonymous instantiation?
    new Route {
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


case class RouteRecord(
  id: RouteId,
  shortName: String,
  longName: String,
  routeType: RouteType,
  agencyId: Option[String] = None,
  description: Option[String] = None,
  url: Option[String] = None,
  color: Option[String] = None,
  textColor: Option[String] = None
) 

sealed trait RouteType {
  val id: Int
  val name: String
}

object RouteType {
  def apply(id: Int): RouteType = 
    id match {
      case 0 => Tram
      case 1 => Subway
      case 2 => Rail
      case 3 => Bus
      case 4 => Ferry
      case 5 => Cablecar
      case 6 => Gondola
      case 7 => Funicular
      case _ => UndefinedRouteType(id, "Unkown")
    }
}

case object Tram       extends RouteType { val id = 0; val name = "Tram" }
case object Subway     extends RouteType { val id = 1; val name = "Subway" }
case object Rail       extends RouteType { val id = 2; val name = "Rail" }
case object Bus        extends RouteType { val id = 3; val name = "Bus" }
case object Ferry      extends RouteType { val id = 4; val name = "Ferry" }
case object Cablecar   extends RouteType { val id = 5; val name = "Cablecar" }
case object Gondola    extends RouteType { val id = 6; val name = "Gondola" }
case object Funicular  extends RouteType { val id = 7; val name = "Funicular" }
case class UndefinedRouteType(id: Int, name: String) extends RouteType

