package geotrellis.transit.services

import geotrellis.transit._
import geotrellis.jetty._

import javax.ws.rs._
import javax.ws.rs.core.Response

@Path("/transitmodes")
class TransitModesService extends ServiceUtil {
  @GET
  @Produces(Array("application/json"))
  def get():Response = {
    val sb = new StringBuilder()
    sb append """
       { "name" : "Walking", "scheduled" : "false" },
       { "name" : "Biking",  "scheduled" : "false" },
"""

    for(mode:String <- Main.context.graph.transitEdgeModes.map(_.service).toSet) {
      sb append s"""
         { "name" : "$mode", "scheduled" : "true" },
"""
    }

    val modesJsonAll = sb.toString
    val modesJson = modesJsonAll.substring(0,modesJsonAll.lastIndexOf(","))

    OK.json(stripJson(s"""
   {
     "modes": [
       $modesJson
      ]
   }
"""))
  }
}
