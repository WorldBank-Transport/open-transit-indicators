package geotrellis.transit

import geotrellis.raster._
import geotrellis.vector._

import spire.syntax.cfor._

object TravelTimeRaster {
  def apply(re: RasterExtent, llRe: RasterExtent, tti: SptInfo, ldelta: Double): Tile = {
    val ldelta2 = ldelta * ldelta
    val SptInfo(spt, duration, Some(ReachableVertices(subindex, extent))) = tti

    val cols = re.cols
    val rows = re.rows
    val tile = ArrayTile.empty(TypeInt, cols, rows)
    val maxDuration = duration - 600

    cfor(0)(_ < cols, _ + 1) { col =>
      cfor(0)(_ < rows, _ + 1) { row =>
        val destLong = llRe.gridColToMap(col)
        val destLat = llRe.gridRowToMap(row)

        val e = Extent(destLong - ldelta, destLat - ldelta, destLong + ldelta, destLat + ldelta)
        val l = subindex.pointsInExtent(e)

        if (!l.isEmpty) {
          var s = 0.0
          var c = 0
          var ws = 0.0
          val length = l.length
          cfor(0)(_ < length, _ + 1) { i =>
            val target = l(i).asInstanceOf[Int]
            val t = spt.travelTimeTo(target).toInt
            val loc = Main.context.graph.location(target)
            val dlat = (destLat - loc.lat)
            val dlong = (destLong - loc.long)
            val d = dlat * dlat + dlong * dlong
            if (d < ldelta2) {
              val w = 1 / d
              s += t * w
              ws += w
              c += 1
            }
          }
          val mean = s / ws
          if (c == 0 || mean.toInt > maxDuration) {
            tile.set(col, row, NODATA)
          } else {
            tile.set(col, row, mean.toInt)
          }
        }
      }
    }

    tile
  }
}
