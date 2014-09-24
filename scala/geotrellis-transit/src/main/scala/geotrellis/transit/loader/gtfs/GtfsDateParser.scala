package geotrellis.transit.loader.gtfs

import geotrellis.transit.Logger
import geotrellis.network.{Time, Location, NamedLocations, NamedLocation}
import geotrellis.network.graph._
import com.github.nscala_time.time.Imports._
import com.azavea.gtfs._

import scala.collection.mutable

object GtfsDateParser {
  val gtfsTimeRegex = """(\d?\d):(\d\d):(\d\d)""".r

  def parse(name: String, gtfsDirectory: String, date: LocalDate): (MutableGraph, NamedLocations) = {

    def getLocalDuration(start: LocalDateTime, end: LocalDateTime): Int = {
      val dur = new Duration(start.toDateTime(DateTimeZone.UTC), end.toDateTime(DateTimeZone.UTC));
      dur.getStandardSeconds.toInt
    }

    def getSimulationTime(ldt: LocalDateTime): Int = 
      getLocalDuration(ldt.withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0), ldt)
    

    def getVertex(stop: Stop, stopsToVertices: mutable.Map[Stop, Vertex], graph: MutableGraph) =
      if(stopsToVertices.contains(stop)) {
        stopsToVertices(stop)
      } else {
        val v = StationVertex(Location(stop.point.x, stop.point.y), stop.name)
        stopsToVertices(stop) = v
        graph += v
        v
      }

    def setEdges(trip: Trip, stopsToVertices: mutable.Map[Stop, Vertex], service: String, graph: MutableGraph): Int = {
      var count = 0
      trip.schedule
        .reduce { (departing, arriving) =>
          val departingVertex = getVertex(departing.stop, stopsToVertices, graph)
          val arrivingVertex = getVertex(arriving.stop, stopsToVertices, graph)

          graph
            .edges(departingVertex)
            .addEdge(
              TransitEdge(
                arrivingVertex,
                service,
                geotrellis.network.Time(getSimulationTime(departing.departureTime)),
                geotrellis.network.Duration(getLocalDuration(departing.departureTime, arriving.arrivalTime))
              )
            )
          count += 1
          arriving
        }
      count
    }

    val g = MutableGraph()
    val records = GtfsRecords.fromFiles(gtfsDirectory)
    val transitSystem = TransitSystemBuilder(records).systemBetween(date, date)

    val (edges, namedLocations) =
      transitSystem.routes.foldLeft(0 -> NamedLocations.EMPTY) { (result, route) =>
        val stopsToVertices = mutable.Map[Stop, Vertex]()

        val edges = Logger.timedCreate("Creating edges for trips...", "Done creating edges.") { () =>
          route.trips.map(setEdges(_, stopsToVertices, name, g)).foldLeft(0)(_ + _)
        }

        val namedLocations =
          NamedLocations(
            for( vertex <- stopsToVertices.values) yield {
              NamedLocation(vertex.name, vertex.location)
            }
          )

        (result._1 + edges, result._2.mergeIn(namedLocations))
      }

    Logger.log(s"$edges edges set.")
    (g, namedLocations)
  }
}
