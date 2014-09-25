package geotrellis.transit.services

import geotrellis.raster._
import geotrellis.raster.render.ColorRamps
import geotrellis.raster.io._
import geotrellis.vector._

import java.io._
import java.util.zip.{ZipEntry, ZipOutputStream}
import scala.io.Source

import com.vividsolutions.jts.geom

import scala.collection.mutable

import com.vividsolutions.jts.{ geom => jts }

// object Projections {
//     val WebMercator = CRS.decode("EPSG:3857")

//     val LatLong = CRS.decode("EPSG:4326")
// }

// object Reproject {
//   private val geometryFactory = new geom.GeometryFactory()

//   private val transformCache:mutable.Map[(Crs,Crs),MathTransform] = 
//     new mutable.HashMap[(Crs,Crs),MathTransform]()
  
//   def cacheTransform(crs1:Crs,crs2:Crs) = {
//     transformCache((crs1,crs2)) = CRS.findMathTransform(crs1,crs2,true)
//   }

//   private def initCache() = {
//     cacheTransform(Projections.LatLong,Projections.WebMercator)
//     cacheTransform(Projections.WebMercator,Projections.LatLong)
//   }

//   initCache()

//   def apply[D](feature:Geometry[D],fromCRS:Crs,toCRS:Crs):Geometry[D] = {
//     if(!transformCache.contains((fromCRS,toCRS))) { cacheTransform(fromCRS,toCRS) }
//     feature.mapGeom( g => 
//       JTS.transform(g, transformCache((fromCRS,toCRS)))
//     )
//   }

//   def apply[D](e:Extent,fromCRS:Crs,toCRS:Crs):Extent = {
//     if(!transformCache.contains((fromCRS,toCRS))) { cacheTransform(fromCRS,toCRS) }    
//     val min  = geometryFactory.createPoint(new geom.Coordinate(e.xmin,e.ymin));
//     val max = geometryFactory.createPoint(new geom.Coordinate(e.xmax,e.ymax));
//     val newMin = 
//       JTS.transform(min, transformCache((fromCRS,toCRS))).asInstanceOf[geom.Point]
//     val newMax = 
//       JTS.transform(max, transformCache((fromCRS,toCRS))).asInstanceOf[geom.Point]
//     val ne = Extent(newMin.getX(),newMin.getY(),newMax.getX(),newMax.getY())
//     Extent(newMin.getX(),newMin.getY(),newMax.getX(),newMax.getY())
//   }
// }

trait ServiceUtil {
  // Constant value to increase the lat\long raster extent by from the bounding box of the
  // reachable vertices of the shortest path tree.
  val ldelta: Float = 0.0018f
  val ldelta2: Float = ldelta * ldelta

  // def reproject(wmX: Double, wmY: Double):(Double,Double) = {
  //   srs.WebMercator.transform(wmX, wmY, srs.LatLng)
  // }
  // def reproject(wmX: Double, wmY: Double) = {
  //   val rp = Reproject(Point(wmX, wmY, 0), Projections.WebMercator, Projections.LatLong)
  //     .asInstanceOf[Point[Int]]
  //     .geom
  //   (rp.getX, rp.getY)
  // }

  def stringToColor(s: String) = {
    val ns =
      if (s.startsWith("0x")) {
        s.substring(2, s.length)
      } else { s }

    val (color, alpha) =
      if (ns.length == 8) {
        (ns.substring(0, ns.length - 2), ns.substring(ns.length - 2, ns.length))
      } else {
        (ns, "FF")
      }

    (Integer.parseInt(color, 16) << 8) + Integer.parseInt(alpha, 16)
  }

  def expandByLDelta(extent:Extent) = 
    Extent(extent.xmin - ldelta,
           extent.ymin - ldelta,
           extent.xmax + ldelta,
           extent.ymax + ldelta)

  def stripJson(json:String) = {
    val sb = new StringBuilder()
    val whitespace_characters = Set(' ','\t','\r','\n')
    var quoted = false
    for(c <- json) {
      if(quoted || !whitespace_characters.contains(c)) {
        sb += c
        if(c == '"') { quoted = !quoted }
      } 
    }

    sb.toString
  }

  def getColorMap(palette:String,breaks:String):Int=>Int = {
    if (palette != "") {
      if (breaks == "") {
        throw new Exception("Must provide breaks with palette")
      }
      val colors = palette.split(",").map(stringToColor).toArray
      val breakpoints = breaks.split(",").map(_.toInt).toArray

      val len = breakpoints.length
      if (len > colors.length) {
        throw new Exception("Breaks must have less than or " +
          "equal the number of colors in the palette.")
      }

      { z =>
        var i = 0
        while (i < len && z > breakpoints(i)) { i += 1 }
        if (i == len) {
          // Allow for the last color in the palette to be
          // for under or over the last break.
          if (len < colors.length) {
            colors(i)
          } else {
            colors(i - 1)
          }
        } else {
          colors(i)
        }
      }
    } else {
      val colorRamp = ColorRamps.HeatmapBlueToYellowToRedSpectrum
      val palette = colorRamp.interpolate(13).colors

      { z =>
        val minutes = z / 60
        minutes match {
          case a if a < 3 => palette(0)
          case a if a < 5 => palette(1)
          case a if a < 8 => palette(3)
          case a if a < 10 => palette(4)
          case a if a < 15 => palette(5)
          case a if a < 20 => palette(6)
          case a if a < 25 => palette(7)
          case a if a < 30 => palette(8)
          case a if a < 40 => palette(9)
          case a if a < 50 => palette(10)
          case a if a < 60 => palette(11)
          case _ => palette(12)
        }
      }
    }
  }

  def deleteRecursively(f:File): Boolean = {
    if (f.isDirectory) f.listFiles match { 
      case null =>
      case xs   => xs foreach deleteRecursively
    }
    f.delete()
  }

  def compressDirectory(directory:File):File = {
    val zipFile = new File(directory,"result.zip")
    val zip = new ZipOutputStream(new FileOutputStream(zipFile))

    for (file <- directory.listFiles) {
      if(zipFile.getName != file.getName) {
        zip.putNextEntry(new ZipEntry(file.getName))
        val in = new BufferedInputStream(new FileInputStream(file))
        var b = in.read()
        while (b > -1) {
          zip.write(b)
          b = in.read()
        }
        in.close()
        zip.closeEntry()
      }
    }
    zip.close()
    zipFile
  }
}
