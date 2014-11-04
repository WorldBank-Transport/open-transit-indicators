package geotrellis.transit.loader.osm

import geotrellis.transit._
import geotrellis.transit.loader.ParseResult
import geotrellis.network._
import geotrellis.network.graph._

import geotrellis.vector._

import scala.collection.mutable

import scala.io.Source
import scala.xml.MetaData
import scala.xml.pull._

import java.io._

object OsmParser {
  def parseLines(lines: Seq[Line]): ParseResult = {
    val graph = MutableGraph()
    val namedLocations = mutable.ListBuffer[NamedLocation]()

    for(line <- lines.dissolve) {
      val (p1, p2) = {
        val points = line.points
        (points.head, points.tail.head)
      }

      val v1 = StreetVertex(Location(p1.x, p2.y), "")
      val v2 = StreetVertex(Location(p1.x, p2.y), "")
      
      if(!graph.contains(v1)) { graph += v1 ; namedLocations += NamedLocation(v1.name, v1.location) }
      if(!graph.contains(v2)) { graph += v2 ; namedLocations += NamedLocation(v2.name, v2.location) }

      val d = Duration((Distance.distance(v1.location, v2.location) / Speeds.walking).toInt)
      graph.edges(v1).addEdge(WalkEdge(v2, d))
    }


    ParseResult(graph, NamedLocations(namedLocations), NamedWays.EMPTY)
  }

  def getAttrib(attrs: MetaData, name: String) = {
    val attr = attrs(name)
    if(attr == null) {
      sys.error(s"Expected attribute $name does not exist")
    }
    if(attr.length > 1) {
      sys.error(s"Expected attribute $name has more than one return.")
    }
    attr(0).text
  }

  def parseNode(attrs: MetaData, nodes: mutable.Map[String, Vertex]) = {
    val id = getAttrib(attrs, "id")
    val lat = getAttrib(attrs, "lat").toDouble
    val lon = getAttrib(attrs, "lon").toDouble

    nodes(id) = StreetVertex(Location(lat, lon), id)
  }

  def addWalkEdge(v1: Vertex, v2: Vertex, w: Duration, graph: MutableGraph, wayInfo: WayInfo) = {
    val edgeSet = graph.edges(v1)
    edgeSet.find(e => e.target == v2 && e.mode == Walking) match {
      case Some(_) => // pass
      case None =>
        edgeSet.addEdge(WalkEdge(v2, w))
    }
  }

  def addBikeEdge(v1: Vertex, v2: Vertex, d: Duration, graph: MutableGraph) = {
    val edgeSet = graph.edges(v1)
    edgeSet.find(e => e.target == v2 && e.mode == Biking) match {
      case Some(_) => // pass
      case None =>
        edgeSet.addEdge(BikeEdge(v2, d))
    }
  }

  def createWayEdges(wayNodes: Seq[Vertex], wayInfo: WayInfo, graph: MutableGraph) = {
    wayNodes.reduceLeft { (v1, v2) =>
      if(!graph.contains(v1)) { graph += v1 }
      if(!graph.contains(v2)) { graph += v2 }
      if(wayInfo.isWalkable) {
        val d = Duration((Distance.distance(v1.location, v2.location) / wayInfo.walkSpeed).toInt)
        addWalkEdge(v1, v2, d, graph, wayInfo)
        addWalkEdge(v2, v1, d, graph, wayInfo)
      }

      if(wayInfo.isBikable) {
        val d = Duration((Distance.distance(v1.location, v2.location) / wayInfo.bikeSpeed).toInt)
        wayInfo.direction match {
          case OneWay =>
            addBikeEdge(v1, v2, d, graph)
          case OneWayReverse =>
            addBikeEdge(v2, v1, d, graph)
          case BothWays =>
            addBikeEdge(v1, v2, d, graph)
            addBikeEdge(v2, v1, d, graph)
        }
      }

      v2 
    }
  }

  def parseWay(parser: XMLEventReader,
               wayAttribs: MetaData,
               nodes: mutable.Map[String, Vertex],
               graph: MutableGraph): List[Vertex] = {
    val wayNodes = mutable.ListBuffer[Vertex]()
    var break = !parser.hasNext
    var wayInfo: WayInfo = null
    var wayEdges = 0

    val wayId = getAttrib(wayAttribs, "id")

    val tags = mutable.Map[String, String]()

    while(!break) {
      parser.next match {
        case EvElemStart(_, "nd", attrs, _) =>
          val id = getAttrib(attrs, "ref")
          if(nodes.contains(id)) {
            val v = nodes(id)
            wayNodes += v
          }
        case EvElemStart(_, "tag", attrs, _) =>
          val k = getAttrib(attrs, "k")
          val v = getAttrib(attrs, "v")
          tags(k) = v
        case EvElemEnd(_, "way") =>
          wayInfo = WayInfo.fromTags(wayId, tags.toMap)
          if(wayInfo.isWalkable) {
            createWayEdges(wayNodes, wayInfo, graph)
            wayEdges += wayNodes.length - 1
          }
          break = true
        case _ => // pass
      }
      break = break || !parser.hasNext
    }

    wayInfo match {
      case _: Walkable => wayNodes.toList
      case x => 
        List[Vertex]()
    }
  }

  def parse(osmPath: String): ParseResult = {
    val nodes = mutable.Map[String, Vertex]()
    var ways = 0
    var wayEdges = 0
    val wayNodes = mutable.Set[Vertex]()

    val graph = MutableGraph()

    Logger.timed("Parsing OSM XML into nodes and edges...",
                 "OSM XML parsing complete.") { () =>
      val source = Source.fromFile(osmPath)

      try {
        val parser = new XMLEventReader(source)
        while(parser.hasNext) {
          parser.next match {
            case EvElemStart(_, "node", attrs, _) =>
              parseNode(attrs, nodes)
            case EvElemStart(_, "way", attrs, _) =>
              val thisWayNodes = parseWay(parser, attrs, nodes, graph)
              if(!thisWayNodes.isEmpty) {
                ways += 1
                wayEdges += thisWayNodes.size
                wayNodes ++= thisWayNodes
              }
            case _ => //pass
          }
        }
      } finally {
        source.close
      }
    }

    Logger.log(s"OSM File contains ${nodes.size} nodes, with ${ways} ways and ${wayEdges} edges.")

    val namedLocations = 
      nodes.keys
           .map { id => NamedLocation(id, nodes(id).location) }

    ParseResult(graph, NamedLocations(namedLocations), NamedWays.EMPTY)
  }
}
