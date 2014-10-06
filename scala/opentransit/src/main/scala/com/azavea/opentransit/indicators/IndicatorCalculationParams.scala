package com.azavea.opentransit.indicators

import geotrellis.vector._

import com.azavea.opentransit.database.{ BoundariesTable, RoadsTable }
import scala.slick.jdbc.JdbcBackend.{Database, DatabaseDef, Session}

import com.azavea.opentransit._
import com.azavea.gtfs.{TransitSystem, Stop}

import scala.collection.mutable

import grizzled.slf4j.Logging

trait StopBuffers {
  def bufferForStop(stop: Stop): Polygon
  def bufferForPeriod(period: SamplePeriod): MultiPolygon
  def totalBuffer: MultiPolygon
}

object StopBuffers {
  def apply(systems: Map[SamplePeriod, TransitSystem], bufferDistance: Double): StopBuffers = {
    val stopMap:mutable.Map[Stop, Polygon] = mutable.Map()
    val periodMap:mutable.Map[SamplePeriod, MultiPolygon] = mutable.Map()

    def calcBufferForStop(stop: Stop): Polygon =
      stop.point.geom.buffer(bufferDistance)

    def calcBufferForPeriod(period: SamplePeriod): MultiPolygon = {
      val system = systems(period)
      val stopBuffers =
        for(
          route <- system.routes;
          trip <- route.trips;
          scheduledStop <- trip.schedule
        ) yield calcBufferForStop(scheduledStop.stop)

      stopBuffers
        .foldLeft(MultiPolygon.EMPTY) { (mp, stopPolygon) =>
          mp.union(stopPolygon) match {
            case MultiPolygonResult(mp) => mp
            case PolygonResult(p) => MultiPolygon(p)
            case _ => mp
          }
        }
      }

    new StopBuffers {
      def bufferForStop(stop: Stop): Polygon = {
        if(!stopMap.contains(stop)) {
          stopMap(stop) = calcBufferForStop(stop)
        }
        stopMap(stop)
      }

      def bufferForPeriod(period: SamplePeriod): MultiPolygon = {
        if(!periodMap.contains(period)) {
          periodMap(period) = calcBufferForPeriod(period)
        }
        periodMap(period)
      }

      lazy val totalBuffer: MultiPolygon =
        systems.keys
          .map(bufferForPeriod(_))
          .foldLeft(MultiPolygon.EMPTY) { (mp, systemBuffer) =>
            mp.union(systemBuffer) match {
              case MultiPolygonResult(mp) => mp
              case PolygonResult(p) => MultiPolygon(p)
              case _ => mp
            }
          }

    }
  }
}

// trait Demographics {
//   def populationUnder(polygon: MultiPolygon): Double
// }

// object Demographics {
//   def apply(db: DatabaseDef): MultiPolygon => Double =
//     db withSession { implicit session =>

//     }
// }

trait Boundaries {
  def cityBoundary: MultiPolygon
  def regionBoundary: MultiPolygon
}

object Boundaries {
  def cityBoundary(id: Int)(implicit session: Session): MultiPolygon =
    BoundariesTable.boundary(id)

  def regionBoundary(id: Int)(implicit session: Session): MultiPolygon =
    BoundariesTable.boundary(id)
}

trait RoadLength {
  def totalRoadLength: Double
}

object RoadLength extends Logging {
  def totalRoadLength(implicit session: Session): Double = {
    debug("Fetching Roads")
    val roadLines: List[Line] = RoadsTable.allRoads
    val distinctRoadLines: Array[Line] =
      (MultiLine(roadLines: _*).union match {
        case MultiLineResult(ml) => ml
        case LineResult(l) => MultiLine(l)
        case NoResult => MultiLine.EMPTY
      }).lines
    val len = distinctRoadLines.map(x => x.length).sum / 1000
    debug(s"Length of roadlines: $len")
    len
  }
}

case class IndicatorSettings(
  povertyLine: Double,
  nearbyBufferDistance: Double,
  maxCommuteTime: Int,
  maxWalkTime: Int,
  averageFare: Double
)

trait IndicatorParams extends StopBuffers
//                         with Demographics
                         with Boundaries
                         with RoadLength {
  val settings: IndicatorSettings
}

/*object IndicatorParams {
  def fromRequest(request: IndicatorCalculationRequest, systems: Map[SamplePeriod, TransitSystem])
           (implicit session: Session): IndicatorParams =
    new IndicatorParams {
      val settings =
        IndicatorSettings(
          request.povertyLine,
          request.nearbyBufferDistance,
          request.maxCommuteTime,
          request.maxWalkTime,
          request.averageFare
        )

      val cityBoundary = Boundaries.cityBoundary(request.cityBoundaryId)
      val regionBoundary = Boundaries.cityBoundary(request.regionBoundaryId)
      val stopBuffers = StopBuffers(systems, request.nearbyBufferDistance)

      // val populationUnder = Demographics(db)
      val totalRoadLength = RoadLength.totalRoadLength
    }
}
*/

object DatabaseIndicatorParamsBuilder {
  def apply(request: IndicatorCalculationRequest, systems: Map[SamplePeriod, TransitSystem], db: DatabaseDef): Map[SamplePeriod, IndicatorParams] =
    db withSession { implicit session =>
      val stopBuffers = StopBuffers(systems, request.nearbyBufferDistance)
      systems.map{case (period, transitSystem) =>

        period -> new IndicatorParams with StopBuffers {

          def bufferForStop(stop: Stop): Polygon = stopBuffers.bufferForStop(stop)
          def bufferForPeriod(period: SamplePeriod): MultiPolygon = stopBuffers.bufferForPeriod(period)
          def totalBuffer: MultiPolygon = stopBuffers.totalBuffer

          val settings =
            IndicatorSettings(
              request.povertyLine,
              request.nearbyBufferDistance,
              request.maxCommuteTime,
              request.maxWalkTime,
              request.averageFare
          )
          val cityBoundary = Boundaries.cityBoundary(request.cityBoundaryId)
          val regionBoundary = Boundaries.cityBoundary(request.regionBoundaryId)

          // val populationUnder = Demographics(db)
          val totalRoadLength = RoadLength.totalRoadLength
        }
      }
    }
  }
