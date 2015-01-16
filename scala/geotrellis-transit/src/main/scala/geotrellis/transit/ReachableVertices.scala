package geotrellis.transit

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.network._
import geotrellis.network.graph._

case class ReachableVertices(index: SpatialIndex[Int], extent: Extent)

object ReachableVertices {
  def fromSpt(spt: ShortestPathTree): Option[ReachableVertices] = {
    var xmin = Double.MaxValue
    var ymin = Double.MaxValue
    var xmax = Double.MinValue
    var ymax = Double.MinValue


    val si = new SpatialIndex[Int](Measure.Dumb)

    for(v <- spt.reachableVertices) {
      val l = Main.context.graph.location(v)
      if (xmin > l.long) {
        xmin = l.long
      }
      if (xmax < l.long) {
        xmax = l.long
      }
      if (ymin > l.lat) {
        ymin = l.lat
      }
      if (ymax < l.lat) {
        ymax = l.lat
      }

      si.insert(v, l.lat, l.long)
    }

    if (xmin == Double.MaxValue)
      None
    else {
      val extent = Extent(xmin, ymin, xmax, ymax)
      Some(ReachableVertices(si, extent))
    }
  }
}
