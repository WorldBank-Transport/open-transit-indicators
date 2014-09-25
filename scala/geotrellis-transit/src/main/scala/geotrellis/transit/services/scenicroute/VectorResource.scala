package geotrellis.transit.services.scenicroute

import javax.ws.rs._
import javax.ws.rs.core.Response

import geotrellis.transit._
import geotrellis.transit.services._

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.vector.json._
import geotrellis.engine._
import geotrellis.engine.op.local._
import geotrellis.engine.op.global._
import geotrellis.jetty._
import geotrellis.network._

import com.vividsolutions.jts.{geom => jts}
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier

import spray.json.DefaultJsonProtocol._

trait VectorResource extends ServiceUtil{
  @GET
  @Path("/json")
  @Produces(Array("application/json"))
  def getVector(
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
    @QueryParam("minStayTimes") 
    minStayTimesString: String,

    @DefaultValue("walking")
    @QueryParam("modes")  
    modes:String,

    @DefaultValue("weekday")
    @QueryParam("schedule")
    schedule:String,
    
    @DefaultValue("500")
    @QueryParam("cols") 
    cols: Int,

    @DefaultValue("500")
    @QueryParam("rows")
    rows: Int,

    @DefaultValue("0.0001")
    @QueryParam("tolerance")
    tolerance:Double): Response = {

    val minStayTimes = minStayTimesString.split(",").map(_.toInt)

    val request = 
      try{
        SptInfoRequest.fromParams(
          latitude,
          longitude,
          time,
          duration,
          modes,
          schedule,
          "departing")
      } catch {
        case e:Exception =>
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

    val multiPolygons:ValueSource[Seq[MultiPolygonFeature[Int]]] =
      sptInfo.vertices match {
        case Some(ReachableVertices(subindex, extent)) =>
          reverseSptInfo.vertices match {
            case Some(ReachableVertices(revSubindex, revExtent)) =>
              val re = RasterExtent(expandByLDelta(extent), cols, rows)
              minStayTimes.map { minStayTime =>
                val r =
                  ScenicRoute.getRaster(re,
                                        re,
                                        sptInfo,
                                        reverseSptInfo,
                                        ldelta,
                                        minStayTime,
                                        duration)

                RasterSource(r, re.extent)
                  .localMapIfSet { z => if(z <= duration) { 1 } else { NODATA } }
                  .toVector
                  // Simplify
                  .map { polygonFeatures =>
                    polygonFeatures.map { polygonFeature =>
                      PolygonFeature(
                        Polygon(TopologyPreservingSimplifier.simplify(polygonFeature.geom.jtsGeom, tolerance).asInstanceOf[jts.Polygon]),
                        polygonFeature.data
                      )
                    }
                   }
                  // Map the individual Vectors into one MultiPolygon
                  .map { polygonFeatures =>
                    MultiPolygonFeature(MultiPolygon(polygonFeatures.map(_.geom)), duration)
                  }
               }
              .toSeq
              .collectSources
              .converge
            case None => 
              ValueSource(Seq(MultiPolygonFeature(MultiPolygon.EMPTY,0)))
          }
        case None => 
          ValueSource(Seq(MultiPolygonFeature(MultiPolygon.EMPTY,0)))
      }

    val geoJson = 
      multiPolygons.map(_.toGeoJson)

    geoJson.run match {
      case Complete(json, h) =>
        OK.json(json)
      case Error(message, failure) =>
        ERROR(message, failure)
    }
  }
}
