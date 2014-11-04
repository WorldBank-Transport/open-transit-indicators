package com.azavea.gtfs

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

  def apply(name: String): RouteType =
    name match {
      case "Tram" => Tram
      case "Subway" => Subway
      case "Rail" => Rail
      case "Bus" => Bus
      case "Ferry" => Ferry
      case "Cablecar" => Cablecar
      case "Gondola" => Gondola
      case "Funicular" => Funicular
      case _ => UndefinedRouteType(-1, name)
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
