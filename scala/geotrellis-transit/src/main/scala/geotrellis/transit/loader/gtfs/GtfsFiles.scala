package geotrellis.transit.loader.gtfs

import geotrellis.transit.loader.{GraphFileSet,ParseResult}

import geotrellis.network.{NamedLocations,NamedLocation,NamedWays}

case class GtfsFiles(name:String,dataPath:String)
extends GraphFileSet {
  def stopsPath = new java.io.File(dataPath,"stops.txt").toString
  def stopTimesPath = new java.io.File(dataPath,"stop_times.txt").toString
  def tripsPath = new java.io.File(dataPath,"trips.txt").toString
  def calendarPath = new java.io.File(dataPath,"calendar.txt").toString

  if(!new java.io.File(stopsPath).exists) { 
    sys.error(s"Stops file $stopsPath does not exist.")
  }

  if(!new java.io.File(stopTimesPath).exists) { 
    sys.error(s"Stop Times file $stopTimesPath does not exist.")
  }

  if(!new java.io.File(calendarPath).exists) { 
    sys.error(s"Calendar file $calendarPath does not exist.")
  }

  if(!new java.io.File(tripsPath).exists) { 
    sys.error(s"Calendar file $tripsPath does not exist.")
  }

  def parse():ParseResult = {
    // val (stops,graph) = GtfsParser.parse(this)
    // val namedLocations = 
    //   NamedLocations(
    //     for(location <- stops.locationToStop.keys) yield {
    //       NamedLocation(stops.locationToStop(location).name,location)
    //     }
    //   )
    // ParseResult(graph,namedLocations,NamedWays.EMPTY)
    ???
  }
}
