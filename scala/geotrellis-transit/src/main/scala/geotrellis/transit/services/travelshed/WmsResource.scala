package geotrellis.transit.services.travelshed

import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.vector.reproject._
import geotrellis.engine._
import geotrellis.engine.render._
import geotrellis.jetty._
import geotrellis.proj4._
import geotrellis.raster.render._

import geotrellis.transit._
import geotrellis.transit.services._

import javax.ws.rs._
import javax.ws.rs.core.Response

trait WmsResource extends ServiceUtil {
  private def getPngOp(
    latitude: Double,
    longitude: Double,
    time: Int,
    duration: Int,
    modes:String,
    schedule:String,
    direction:String,
    bbox: String,
    cols: Int,
    rows: Int,
    resolutionFactor: Int)(colorRasterFunc: Tile => Tile): ValueSource[Png] = {

    val request = 
      SptInfoRequest.fromParams(
        latitude,
        longitude,
        time,
        duration,
        modes,
        schedule,
        direction)

    val sptInfo = SptInfoCache.get(request)

    val extent = Extent.fromString(bbox)
    val llExtent = extent.reproject(WebMercator, LatLng)

    val re = RasterExtent(extent, cols, rows)
    val llRe = RasterExtent(llExtent, cols, rows)


    sptInfo match {
      case SptInfo(spt, _, Some(ReachableVertices(subindex, extent))) =>
        val newRe =
          re.withResolution(re.cellwidth * resolutionFactor, re.cellheight * resolutionFactor)
        val newllRe =
          llRe.withResolution(llRe.cellwidth * resolutionFactor, llRe.cellheight * resolutionFactor)

        val cols = newRe.cols
        val rows = newRe.rows

        val r = 
          llRe.extent.intersection(expandByLDelta(extent)) match {
            case Some(ie) => 
              TravelTimeRaster(newRe, newllRe, sptInfo,ldelta)
            case None => 
              ArrayTile.empty(TypeInt, newRe.cols, newRe.rows)
          }

        RasterSource(colorRasterFunc(r), newRe.extent)
          .warp(RasterExtent(newRe.extent, cols, rows))
          .renderPng
      case _ =>
        RasterSource(ArrayTile.empty(TypeInt, re.cols, re.rows), re.extent)
          .renderPng(ColorRamps.BlueToRed)
    }
  }

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
    
    @DefaultValue("0")
    @QueryParam("time") 
    time: Int,
    
    @DefaultValue("1800")
    @QueryParam("duration") 
    duration: Int,

    @DefaultValue("walking")
    @QueryParam("modes")  
    modes:String,

    @DefaultValue("weekday")
    @QueryParam("schedule")
    schedule:String,
 
    @DefaultValue("departing")
    @QueryParam("direction")
    direction:String,

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
    try {
      val colorMap =
        try {
          getColorMap(palette,breaks)
        } catch {
          case e:Exception =>
            return ERROR(e.getMessage)
        }

      val png = getPngOp(
        latitude,
        longitude,
        time,
        duration,
        modes,
        schedule,
        direction,
        bbox,
        cols,
        rows,
        resolutionFactor)(_.mapIfSet(colorMap))

      png.run match {
        case Complete(img, h) =>
          OK.png(img)
        case Error(message, failure) =>
          ERROR(message, failure)
      }
    } catch {
      case e:Exception =>
        return ERROR(e.getMessage)
    }
  }

  @GET
  @Path("/wmsdata")
  @Produces(Array("image/png"))
  def getWmsData(
    @DefaultValue("39.957572")
    @QueryParam("latitude") 
    latitude: Double,
    
    @DefaultValue("-75.161782")
    @QueryParam("longitude") 
    longitude: Double,
    
    @DefaultValue("0")
    @QueryParam("time") 
    time: Int,
    
    @DefaultValue("1800")
    @QueryParam("duration") 
    duration: Int,

    @DefaultValue("walking")
    @QueryParam("modes")  
    modes:String,

    @DefaultValue("weekday")
    @QueryParam("schedule")
    schedule:String,
 
    @DefaultValue("departing")
    @QueryParam("direction")
    direction:String,

    @QueryParam("bbox") 
    bbox: String,

    @DefaultValue("256")
    @QueryParam("cols") 
    cols: Int,

    @DefaultValue("256")
    @QueryParam("rows") 
    rows: Int,

    @DefaultValue("3")
    @QueryParam("resolutionFactor")
    resolutionFactor: Int): Response = {

    try {
      val png = getPngOp(
        latitude,
        longitude,
        time,
        duration,
        modes,
        schedule,
        direction,
        bbox,
        cols,
        rows,
        resolutionFactor)({ r =>
          r.map { z =>
            if (z == NODATA) 0 else {
              // encode seconds in RGBA color values: 0xRRGGBBAA.

              // If you disregard the alpha channel,
              // you can think of this as encoding the value in base 255:
              // B = x * 1
              // G = x * 255
              // R = x * 255 * 255
              val b = (z % 255) << 8
              val g = (z / 255).toInt << 16
              val r = (z / (255 * 255)).toInt << 24

              // Alpha channel is always set to 255 to avoid the values getting garbled
              // by browser optimizations.
              r | g | b | 0xff
            }
          }
        })

      png.run match {
        case Complete(img, h) =>
          OK.png(img)
        case Error(message, failure) =>
          ERROR(message, failure)
      }
    } catch {
      case e:Exception =>
        return ERROR(e.getMessage)
    }
  }
}

