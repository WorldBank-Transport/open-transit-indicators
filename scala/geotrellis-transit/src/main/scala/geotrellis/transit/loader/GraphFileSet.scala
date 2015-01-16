package geotrellis.transit.loader

import geotrellis.network.{NamedLocations,NamedWays}
import geotrellis.network.graph.MutableGraph

case class ParseResult(graph:MutableGraph,namedLocations:NamedLocations,namedWays:NamedWays) {
  def merge(other:ParseResult) = {
    ParseResult(MutableGraph.merge(graph,other.graph),
                namedLocations.mergeIn(other.namedLocations),
                namedWays.mergeIn(other.namedWays))
  }
}

trait GraphFileSet {
  val name:String

  def parse():ParseResult
}
