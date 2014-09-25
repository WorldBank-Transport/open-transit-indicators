package geotrellis.transit

import geotrellis.network._
import geotrellis.network.graph._

case class SptInfoRequest(lat: Double, 
                          lng:Double, 
                          time:Time, 
                          duration:Duration,
                          modes:Seq[TransitMode],
                          departing:Boolean)
object SptInfoRequest {
  val availableModes = Main.context.graph.transitEdgeModes
                                         .map(_.service)
                                         .toSet
  val modesStr = (List("walking","biking") ++ availableModes.map(_.toLowerCase)).mkString(", ")

  def fromParams(latitude:Double,
                 longitude:Double,
                 timeString:Int,
                 durationString:Int,
                 modesString:String,
                 schedule:String,
                 direction:String):SptInfoRequest = {
      val lat = latitude.toDouble
      val long = longitude.toDouble
      val time = Time(timeString.toInt)
      val duration = Duration(durationString.toInt + 600) 

      val modes = 
        (for(mode <- modesString.split(",")) yield {
          mode.toLowerCase match {
            case "walking" => Walking
            case "biking" => Biking
            case s =>
              availableModes.find(_.toLowerCase == s) match {
                case Some(service) =>
                  ScheduledTransit(
                    service,
                    schedule match {
                      case "weekday" => WeekDaySchedule
                      case "saturday" => DaySchedule(Saturday)
                      case "sunday" => DaySchedule(Sunday)
                      case _ =>
                        throw new Exception(s"Unknown schedule. Choose on of weekday,saturday, or sunday.")
                    })
                case None =>
                    throw new Exception(s"Unknown mode. Choose one or more from $modesStr.")
              }
          }
        })

    val departing = direction != "arriving"
      
    SptInfoRequest(lat,long,time,duration,modes,departing)
  }
}
