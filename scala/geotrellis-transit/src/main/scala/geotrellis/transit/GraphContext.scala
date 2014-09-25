package geotrellis.transit

import geotrellis.network._
import geotrellis.network.graph._

import java.io._

case class GraphContext(graph:TransitGraph,
                        index:SpatialIndex[Int],
                        namedLocations:NamedLocations,
                        namedWays:NamedWays)

object GraphContext {
  def getContext(path:String) = {
    if(!new File(path).exists) {
      sys.error("Graph data does not exists.")
    } else {
      Logger.timedCreate("Reading graph file object...","Read graph object") { () =>

        val input = new ObjectInputStream(new FileInputStream(path)) {
          override def resolveClass(desc: java.io.ObjectStreamClass): Class[_] = {
            try { Class.forName(desc.getName, false, getClass.getClassLoader) }
            catch { case ex: ClassNotFoundException => super.resolveClass(desc) }
          }
        }

        val (graph,namedLocations,namedWays) =
          try {
            input.readObject().asInstanceOf[(TransitGraph,NamedLocations,NamedWays)]
          }
          finally{
            input.close()
          }

        val index = createSpatialIndex(graph)

        GraphContext(graph,index,namedLocations,namedWays)
      }
    }
  }

  def createSpatialIndex(graph:TransitGraph) = 
    Logger.timedCreate("Creating spatial index...", "Spatial index created.") { () =>
      SpatialIndex(0 until graph.vertexCount) { v => 
        val l = graph.location(v)
        (l.lat,l.long)
      }
    }
}
