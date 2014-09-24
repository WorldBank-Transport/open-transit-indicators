package geotrellis.transit.services.travelshed

import javax.ws.rs._
import javax.ws.rs.core.Response

import geotrellis.transit._
import geotrellis.transit.services._

import geotrellis.raster._
import geotrellis.engine._
import geotrellis.engine.op.local._
import geotrellis.engine.op.global._
import geotrellis.jetty._
import geotrellis.vector._
import geotrellis.vector.json._

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
    
    @DefaultValue("0")
    @QueryParam("time") 
    time: Int,
    
    @DefaultValue("1800")
    @QueryParam("durations")
    durationsString: String,

    @DefaultValue("walking")
    @QueryParam("modes")  
    modes: String,

    @DefaultValue("weekday")
    @QueryParam("schedule")
    schedule: String,
 
    @DefaultValue("departing")
    @QueryParam("direction")
    direction: String,

    @DefaultValue("500")
    @QueryParam("cols") 
    cols: Int,

    @DefaultValue("500")
    @QueryParam("rows") 
    rows: Int,

    @DefaultValue("0.0001")
    @QueryParam("tolerance") 
    tolerance: Double): Response = {

    val durations = durationsString.split(", ").map(_.toInt)
    val maxDuration = durations.foldLeft(0)(math.max(_, _))

    val request = 
      try{
        SptInfoRequest.fromParams(
          latitude,
          longitude,
          time,
          maxDuration,
          modes,
          schedule,
          direction)
      } catch {
        case e: Exception =>
          return ERROR(e.getMessage)
      }

    val sptInfo = SptInfoCache.get(request)

    val multiPolygons: ValueSource[Seq[MultiPolygonFeature[Int]]] =
      sptInfo.vertices match {
        case Some(ReachableVertices(subindex, extent)) =>
          val re = RasterExtent(expandByLDelta(extent), cols, rows)
          val r = TravelTimeRaster(re, re, sptInfo, ldelta)
          (for(duration <- durations) yield {
            RasterSource(r, re.extent)
              // Set all relevant times to 1, everything else to NODATA
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
           })
          .toSeq
          .collectSources
          .converge
        case None => ValueSource(Seq(MultiPolygonFeature(MultiPolygon.EMPTY, 0)))
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
