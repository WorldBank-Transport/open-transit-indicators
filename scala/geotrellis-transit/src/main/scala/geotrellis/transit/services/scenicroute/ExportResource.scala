package geotrellis.transit.services.scenicroute

import geotrellis.transit._
import geotrellis.transit.services._

import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.network._
import geotrellis.jetty._
import geotrellis.raster.io.arg.ArgWriter
import geotrellis.raster.io.geotiff

import javax.ws.rs._
import javax.ws.rs.core

import java.io.{File, FileInputStream}
import com.google.common.io.Files

trait ExportResource extends ServiceUtil {
  @GET
  @Path("/export")
  @Produces(Array("application/octet-stream"))
  def getExport(
    @DefaultValue("39.957572")
    @QueryParam("latitude") 
    latitude: Double,
        
    @DefaultValue("-75.161782")
    @QueryParam("longitude") 
    longitude: Double,

    @DefaultValue("39.987572")
    @QueryParam("destlatitude") 
    destlatitude: Double,

    @DefaultValue("-75.261782")
    @QueryParam("destlongitude") 
    destlongitude: Double,
    
    @DefaultValue("0")
    @QueryParam("time") 
    time: Int,
    
    @DefaultValue("1800")
    @QueryParam("duration") 
    duration: Int,

    @DefaultValue("0")
    @QueryParam("minStayTime") 
    minStayTime: Int,

    @DefaultValue("walking")
    @QueryParam("modes")  
    modes: String,

    @DefaultValue("weekday")
    @QueryParam("schedule")
    schedule: String,

    @QueryParam("bbox") 
    bbox: String,

    @DefaultValue("256")
    @QueryParam("cols") 
    cols: Int,

    @DefaultValue("256")
    @QueryParam("rows") 
    rows: Int,

    @DefaultValue("tiff")
    @QueryParam("format")
    format: String): core.Response = {

    val request =
      try {
        SptInfoRequest.fromParams(
          latitude,
          longitude,
          time,
          duration,
          modes,
          schedule,
          "departing")
      } catch {
        case e: Exception =>
          return ERROR(e.getMessage)
      }

    val sptInfo = SptInfoCache.get(request)

    // Get arrival request
    val reverseSptInfoRequest = 
      SptInfoRequest(destlatitude,
                     destlongitude,
                     Time(request.time.toInt + request.duration.toInt),
                     request.duration,
                     request.modes,
                     !request.departing)
    val reverseSptInfo = SptInfoCache.get(reverseSptInfoRequest)

    val re = RasterExtent(Extent.fromString(bbox), cols, rows)

    val (spt, subindex, extent) = sptInfo match {
      case SptInfo(spt, duration, Some(ReachableVertices(subindex, extent))) => (spt, subindex, extent)
      case _ => return ERROR("Invalid SptInfo in cache.")
    }

    val (revSpt, revSubindex, revExtent) = reverseSptInfo match {
      case SptInfo(revSpt, _, Some(ReachableVertices(revSubindex, revExtent))) => (revSpt, revSubindex, revExtent)
      case _ => return ERROR("Invalid SptInfo in cache.")
    }

    val d = Files.createTempDir()

    try {
      val r =
        re.extent.intersection(expandByLDelta(extent)) match {
          case Some(_) =>
            re.extent.intersection(expandByLDelta(revExtent)) match {
              case Some(_) =>
                ScenicRoute.getRaster(re,
                  re,
                  sptInfo,
                  reverseSptInfo,
                  ldelta,
                  minStayTime,
                  duration)
              case None => ArrayTile.empty(TypeInt, re.cols, re.rows)
            }
          case None => ArrayTile.empty(TypeInt, re.cols, re.rows)
        }

      val name = s"scenicroute"

      if(format == "arg") {
        ArgWriter(TypeInt).write(new File(d, s"$name.arg").getAbsolutePath, r, re.extent, name)
      } else {
        geotiff.Encoder.writePath(new File(d, s"$name.tif").getAbsolutePath, r, re, geotiff.Settings.int32)
      }

      val zipFile = compressDirectory(d)

      val in = new FileInputStream(zipFile)
      try {
        val bytes = new Array[Byte](zipFile.length.toInt)
        in.read(bytes)
        in.close()
        Response.ok("application/octet-stream").data(bytes)
      } finally {
        in.close()
      }
    } finally {
      deleteRecursively(d)
    }
  }
}
