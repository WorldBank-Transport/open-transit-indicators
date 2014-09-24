package geotrellis.transit.services

import geotrellis.transit._

import geotrellis._
import geotrellis.network._
import geotrellis.jetty._

import javax.ws.rs._
import javax.ws.rs.core.Response

@Path("/reachable")
class ReachableService extends ServiceUtil {
  val latLongRegex = """\[(-?\d+\.?\d*),(-?\d+\.?\d*)\]""".r

  case class LocationInfo(lat:Double,lng:Double,vertex:Int)
  case class ReachableInfo(location:LocationInfo,duration:Duration) {
    val reachable = duration != Duration.UNREACHABLE
    def toJson() = {
      s"""{ "location": [${location.lat},${location.lng}], 
            "reachable": "$reachable", 
            "duration": ${duration.toInt} }"""
    }
  }

  @GET
  @Produces(Array("application/json"))
  def getReachable(
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

    @DefaultValue("[39.96,-75.17],[39.85,-75.15]")
    @QueryParam("points")
    points:String

): Response = {
    // Parse points
    val errorMsg = "Invalid 'points' parameter format. e.g. [39.96,-75.17],[39.85,-75.15]"
    val locations = 
      try {
        (for(latLongRegex(latString,lngString) <- latLongRegex findAllIn points) yield {
          val lat = latString.toDouble
          val lng = lngString.toDouble
          val v = Main.context.index.nearest(lat, lng)
          LocationInfo(lat,lng,v)
        }).toList
      } catch {
        case _:Exception => return ERROR(errorMsg)
      }

    val request = 
      try {
        SptInfoRequest.fromParams(
          latitude,
          longitude,
          time,
          duration,
          modes,
          schedule,
          direction)
      } catch {
        case e:Exception => 
          return ERROR(e.getMessage)
      }

    val sptInfo = SptInfoCache.get(request)

    val results = 
      (for(l <- locations) yield {
        ReachableInfo(l,sptInfo.spt.travelTimeTo(l.vertex))
      }).toSeq
        .map(_.toJson)
        .reduce(_+","+_)


    OK.json(s"""
{
  "result" : [ $results ]
}""")
  }
}
