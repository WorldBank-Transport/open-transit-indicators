package geotrellis.transit.services.scenicroute

import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.vector.reproject._
import geotrellis.proj4._
import geotrellis.engine._
import geotrellis.engine.render._
import geotrellis.jetty._
import geotrellis.raster.render.ColorRamps
import geotrellis.raster.op._
import geotrellis.network._

import geotrellis.transit._
import geotrellis.transit.services._

import javax.ws.rs._
import javax.ws.rs.core.Response

trait WmsResource extends ServiceUtil {
  @GET
  @Path("/wms")
  @Produces(Array("image/png"))
  def getWms(
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

    @DefaultValue("")
    @QueryParam("palette") 
    palette: String,

    @DefaultValue("")
    @QueryParam("breaks")
    breaks: String,

    @DefaultValue("3")
    @QueryParam("resolutionFactor")
    resolutionFactor: Int): Response = {

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

    val extent = Extent.fromString(bbox)

    val llExtent = extent.reproject(WebMercator, LatLng)

    val re = RasterExtent(extent, cols, rows)
    val llRe = RasterExtent(llExtent, cols, rows)

    val png = 
      sptInfo match {
        case SptInfo(spt, _, Some(ReachableVertices(subindex, extent))) =>
          reverseSptInfo match {
            case SptInfo(revSpt, _, Some(ReachableVertices(revSubindex, revExtent))) =>
              val newRe =
                re.withResolution(re.cellwidth * resolutionFactor,
                  re.cellheight * resolutionFactor)
              val newllRe =
                llRe.withResolution(llRe.cellwidth * resolutionFactor,
                  llRe.cellheight * resolutionFactor)

              val cols = newRe.cols
              val rows = newRe.rows
              val raster = 
                llRe.extent.intersection(expandByLDelta(extent)) match {
                  case Some(_) =>
                    llRe.extent.intersection(expandByLDelta(revExtent)) match {
                      case Some(_) =>
                        ScenicRoute.getRaster(newRe,
                          newllRe,
                          sptInfo,
                          reverseSptInfo,
                          ldelta,
                          minStayTime,
                          duration)
                      case None => ArrayTile.empty(TypeInt, newRe.cols, newRe.rows)
                    }
                  case None => ArrayTile.empty(TypeInt, newRe.cols, newRe.rows)
                }

              val colorMap = 
                try {
                  getColorMap(palette, breaks)
                } catch {
                  case e: Exception =>
                    return ERROR(e.getMessage)
                }

              RasterSource(raster.mapIfSet(colorMap), newllRe.extent)
                .warp(RasterExtent(newRe.extent, cols, rows))
                .renderPng
            case _ =>
              RasterSource(ArrayTile.empty(TypeInt, re.cols, re.rows), re.extent)
                .renderPng(ColorRamps.BlueToRed)
          }
        case _ =>
          RasterSource(ArrayTile.empty(TypeInt, re.cols, re.rows), re.extent)
            .renderPng(ColorRamps.BlueToRed)
      }

    png.run match {
      case Complete(img, h) =>
        OK.png(img.bytes)
      case Error(message, failure) =>
        ERROR(message, failure)
    }
  }
}

