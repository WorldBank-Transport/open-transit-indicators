package geotrellis.transit

import geotrellis.transit._

import geotrellis.network._
import geotrellis.network.graph._


case class SptInfo(spt: ShortestPathTree, maxDuration: Int, vertices: Option[ReachableVertices]) {
  def isEmpty = !vertices.isDefined
}

import javax.xml.bind.annotation._
import scala.reflect.BeanProperty

object SptInfo {
  def apply(request:SptInfoRequest): SptInfo = {
    val SptInfoRequest(lat,lng,time,duration,modes,departing) = request
    val startVertex = Main.context.index.nearest(lat, lng)
    val spt =
      geotrellis.transit.Logger.timedCreate("Creating shortest path tree...",
        "Shortest Path Tree created.") { () =>
        if(departing) {
          ShortestPathTree.departure(startVertex, time, Main.context.graph, duration,modes:_*)
        } else {
          ShortestPathTree.arrival(startVertex, time, Main.context.graph, duration,modes:_*)
        }
      }

    SptInfo(spt, duration.toInt, ReachableVertices.fromSpt(spt))
  }

}
