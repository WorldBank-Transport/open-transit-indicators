// package geotrellis.transit.loader.gtfs

// import geotrellis.transit.loader.gtfs.model._
// import geotrellis.transit.Logger
// import geotrellis.network.{Time,Duration,Location}
// import geotrellis.network.graph.{Vertex, MutableGraph}
// import geotrellis.network._

// import scala.collection.mutable

// object GtfsParser {
//   val gtfsTimeRegex = """(\d?\d):(\d\d):(\d\d)""".r

//   def parse(files:GtfsFiles):(Stops,MutableGraph) = {
//     val g = MutableGraph()

//     val serviceIds = parseServiceIds(files.calendarPath)

//     val stops = parseStops(files.stopsPath)
//     val trips = parseTrips(files.tripsPath,serviceIds)
//     parseStopTimes(stops, trips, files.stopTimesPath)

//     val stopsToVertices = mutable.Map[Stop,Vertex]()

//     val edges = Logger.timedCreate("Creating edges for trips...","Done creating edges.") { () => 
//       trips.values.map(_.setEdges(stopsToVertices,files.name,g)).foldLeft(0)(_+_)
//     }
//     Logger.log(s"$edges edges set.")
//     (stops,g)
//   }

//   def parseServiceIds(calendarPath:String):Map[String,WeeklySchedule] = {
//     (for(row <- Csv.fromPath(calendarPath)) yield {
//       if(row("monday") != "0") {
//         (row("service_id"), WeekDaySchedule)
//       } else if(row("saturday") != "0") {
//         (row("service_id"), DaySchedule(Saturday))
//       } else {
//         (row("service_id"), DaySchedule(Sunday))
//       }
//     }).toMap
//   }

//   def parseStops(stopsPath:String):Stops = {
//     val stops = new Stops()
//     Logger.timed("Parsing stops file...","Finished parsing stops.") { () =>
//       for(row <- Csv.fromPath(stopsPath)) {
//         val id = row("stop_id")
//         val name = row("stop_name")
//         val lat = row("stop_lat").toDouble
//         val long = row("stop_lon").toDouble
//         stops.add(Stop(id,name,Location(lat,long)))
//       }
//     }
//     Logger.log(s"${stops.count} stops parsed.")
//     stops
//   }

//   def parseTrips(tripsPath:String,serviceIds:Map[String,WeeklySchedule]) = {
//     val trips = mutable.Map[String,Trip]()
//     Logger.timed("Parsing trips file...","Finished parsing trips.") { () =>
//       for(row <- Csv.fromPath(tripsPath)) {
//         val serviceId = row("service_id")
//         if(serviceIds.contains(serviceId)) {
//           val tripId = row("trip_id")
//           trips(tripId) = new Trip(tripId,serviceIds(serviceId))
//         }
//       }
//     }
//     trips.toMap
//   }

//   def parseStopTimes(stops:Stops, trips:Map[String,Trip], stopTimesPath:String) = {
//     var count = 0
//     Logger.timed("Parsing stop times file...","Finished parsing stop times.") { () =>
//       for(row <- Csv.fromPath(stopTimesPath)) {
//         val tripId = row("trip_id")
//         if(trips.contains(tripId)) {
//           val trip = trips(tripId)
//           val stopId = row("stop_id")

//           if(!stops.contains(stopId)) {
//             sys.error(s"Stop Times file at $stopTimesPath contains stop $stopId " +
//               "that is not included in the stops file.")
//           }

//           val stop = stops.get(stopId)
          
//           val seq = row("stop_sequence").toInt
//           val arriveTime = parseTime(row("arrival_time"))
//           val departTime = parseTime(row("departure_time"))
          
//           trip.stopTimes(seq) = StopTime(stop,arriveTime,departTime)
//           count += 1
//         }
//       }
//     }
//     Logger.log(s"$count stop times parsed for ${trips.size} trips.")
//   }

//   def parseTime(s:String):Time = {
//     val gtfsTimeRegex(hour,minute,second) = s
//     Time(second.toInt + (minute.toInt*60) + (hour.toInt*3600))
//   }
// }
