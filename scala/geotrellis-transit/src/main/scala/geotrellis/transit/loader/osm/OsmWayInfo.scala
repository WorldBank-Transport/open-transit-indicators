package geotrellis.transit.loader.osm

import geotrellis.network._

trait WayInfo {
  val wayId:String

  def isWalkable:Boolean
  def walkSpeed:Double

  val isBikable:Boolean
  val bikeSpeed:Double

  private var _direction:WayDirection = BothWays
  def direction = _direction

  val tags:Map[String,String]
}

abstract sealed class WayDirection
case object OneWay extends WayDirection
case object BothWays extends WayDirection
case object OneWayReverse extends WayDirection

case object Impassable extends WayInfo {
  val wayId = "IMPASSABLE"
  val isWalkable = false
  val isBikable = false

  val walkSpeed = 0.0
  val bikeSpeed = 0.0
  val tags = Map[String,String]()
}

trait Walkable {
  val isWalkable = true

  val walkSpeed = Speeds.walking
}

trait Bikable {
  val isBikable = true

  val bikeSpeed = Speeds.biking
}

case class WalkOrBike(wayId:String,tags:Map[String,String]) extends WayInfo 
                          with Walkable
                          with Bikable

case class WalkOnly(wayId:String,tags:Map[String,String]) extends WayInfo 
                        with Walkable {
  val isBikable = false
  val bikeSpeed = 0.0
}

case class BikeOnly(wayId:String,tags:Map[String,String]) extends WayInfo 
                        with Bikable {
  val isWalkable = false
  val walkSpeed = 0.0
}

object WayInfo {
  // http://wiki.openstreetmap.org/wiki/Key:oneway
  private val oneWayTrueValues = Set("yes","true","1")
  private val oneWayReverseValues = Set("-1","reverse")

  def fromTags(wayId:String,tags:Map[String,String]):WayInfo = {
    var info:WayInfo = null

    if(tags.contains("highway")) {
      info = forHighwayType(wayId,tags)
    }

    if(info == null) {
      if(tags.contains("public_transport")) {
        if(tags("public_transport") == "platform") {
          info = WalkOnly(wayId,tags)
        }
      }
    }

    if(info == null) {
      if(tags.contains("railway")) {
        if(tags("railway") == "platform") {
          info = WalkOnly(wayId,tags)
        }
      }
    }

    info match {
      case null => Impassable
      case Impassable => Impassable
      case _ =>
        // Check for one-way
        if(tags.contains("oneway")) {
          val oneway = tags("oneway")
          info._direction = 
            if(oneWayTrueValues.contains(oneway)) {
              OneWay
            } else if (oneWayReverseValues.contains(oneway)) {
              OneWayReverse
            } else {
              BothWays
            }
        }
        info
    }
  }

  // http://wiki.openstreetmap.org/wiki/Map_Features#Highway
  def forHighwayType(wayId:String,tags:Map[String,String]):WayInfo =
    tags("highway") match {
      case "motorway" => Impassable
      case "motorway_link" => Impassable
      case "trunk" => Impassable
      case "trunk_link" => Impassable
      case "primary" => WalkOrBike(wayId,tags)
      case "primary_link" => WalkOrBike(wayId,tags)
      case "secondary" => WalkOrBike(wayId,tags)
      case "secondary_link" => WalkOrBike(wayId,tags)
      case "tertiary" => WalkOrBike(wayId,tags)
      case "tertiary_link" => WalkOrBike(wayId,tags)
      case "living_street" => WalkOrBike(wayId,tags)
      case "pedestrian" => WalkOrBike(wayId,tags)
      case "residential" => WalkOrBike(wayId,tags)
      case "unclassified" => WalkOrBike(wayId,tags)
      case "service" => WalkOrBike(wayId,tags)
      case "track" => WalkOrBike(wayId,tags)
      case "bus_guideway" => Impassable
      case "raceway" => Impassable
      case "road" => WalkOrBike(wayId,tags)
      case "path" => WalkOrBike(wayId,tags)
      case "footway" => WalkOrBike(wayId,tags)
      case "cycleway" => WalkOrBike(wayId,tags)
      case "bridleway" => WalkOrBike(wayId,tags)
      case "steps" => WalkOnly(wayId,tags)
      case "proposed" => Impassable
      case "construction" => Impassable
      case "bus_stop" => WalkOnly(wayId,tags)
      case "crossing" => WalkOrBike(wayId,tags)
      case "emergency_access_point" => Impassable
      case "escape" => Impassable
      case "give_way" => Impassable
      case "mini_roundabout" => WalkOrBike(wayId,tags)
      case "motorway_junction" => Impassable
      case "parking" => WalkOnly(wayId,tags)
      case _ => Impassable
    }
}
