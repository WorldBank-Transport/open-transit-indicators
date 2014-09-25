package geotrellis.transit.loader.gtfs

import geotrellis.transit.loader.{GraphFileSet, ParseResult}

import geotrellis.network.{NamedLocations, NamedLocation, NamedWays}

import com.github.nscala_time.time.Imports._

// new LocalDate(2013, 1, 1))
case class GtfsDateFiles(name: String, dataPath: String, date: LocalDate)
    extends GraphFileSet {
  def parse(): ParseResult = {
    val (graph, namedLocations) = GtfsDateParser.parse(name, dataPath, date)
    ParseResult(graph, namedLocations, NamedWays.EMPTY)
  }
}
