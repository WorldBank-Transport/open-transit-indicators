package com.azavea.opentransit.indicators.parameters

import com.azavea.opentransit.indicators._
import com.azavea.opentransit._
import com.azavea.gtfs._
import com.azavea.gtfs.io.database.DatabaseStops

import geotrellis.slick._
import geotrellis.vector._

import com.azavea.gtfs.io.database.{ DatabaseGtfsRecords, DefaultProfile }

import com.vividsolutions.jts.geom.{MultiPolygon => JTSMultiPolygon, Polygon => JTSPolygon}
import com.vividsolutions.jts.operation.union._

import scala.collection.JavaConversions._
import scala.collection.mutable

import scala.slick.jdbc.JdbcBackend.{Database, DatabaseDef, Session}
import com.typesafe.config.{ConfigFactory, Config}

/**
 * Trait used to populate parameters with StopBuffer information
 */
trait StopBuffers {
  def bufferForStop(stop: Stop): Projected[MultiPolygon]
  def bufferForStops(stops: Seq[Stop]): Projected[MultiPolygon]
  def bufferForPeriod(period: SamplePeriod): Projected[MultiPolygon]
}

object StopBuffers {
  def apply(systems: Map[SamplePeriod, TransitSystem], bufferDistance: Double, db: DatabaseDef): StopBuffers = {

    // Get buffers up front from database
    val stopMap = db withSession { implicit session =>
      val config = ConfigFactory.load
      val dbGeomNameUtm = config.getString("database.geom-name-utm")
      val databaseStops = new DatabaseStops with DefaultProfile {
        override val geomColumnName = dbGeomNameUtm }
      databaseStops.getStopBuffers(bufferDistance.asInstanceOf[Float])
    }

    val periodMap:mutable.Map[SamplePeriod, Projected[MultiPolygon]] = mutable.Map()

    // calculate buffers for a given sequence of stops (useful for populations served by trips/routes/etc
    def calcBufferForStops(stops: Seq[Stop]): Projected[MultiPolygon] = {
      val stopBuffers = stops.distinct.map(stop => stopMap(stop.id)).map(_.jtsGeom)
      val stopSrid = stopBuffers.head.getSRID

      val union = new CascadedPolygonUnion(stopBuffers)
      val unionedGeometry = union.union()
      val multipolygon = unionedGeometry match {
        case p:JTSPolygon => MultiPolygon(Polygon(p))
        case mp:JTSMultiPolygon => MultiPolygon(mp)
      }
      Projected(multipolygon, stopSrid)
    }


    // Calculate combined buffers for entire period
    def calcBufferForPeriod(period: SamplePeriod): Projected[MultiPolygon] = {
      val system = systems(period)
      val allStops =
        for(
          route <- system.routes;
          trip <- route.trips;
          scheduledStop <- trip.schedule
        ) yield scheduledStop.stop

      val stopBuffers = allStops.distinct.map(stop => stopMap(stop.id)).map(_.jtsGeom)
      val stopSrid = stopBuffers.head.getSRID

      val union = new CascadedPolygonUnion(stopBuffers)
      val unionedGeometry = union.union()
      val multipolygon = unionedGeometry match {
        case p:JTSPolygon => MultiPolygon(Polygon(p))
        case mp:JTSMultiPolygon => MultiPolygon(mp)
      }
      Projected(multipolygon, stopSrid)
    }

    new StopBuffers {
      // Return buffer for a stop
      def bufferForStop(stop: Stop): Projected[MultiPolygon] =
        Projected(
            MultiPolygon(stopMap(stop.id).geom),
            stopMap(stop.id).srid
        )
      // Return buffers for a sequence of stops
      def bufferForStops(stops: Seq[Stop]): Projected[MultiPolygon] =
        calcBufferForStops(stops)
      // Return buffers for a period
      def bufferForPeriod(period: SamplePeriod): Projected[MultiPolygon] = {
        if(!periodMap.contains(period)) {
          periodMap(period) = calcBufferForPeriod(period)
        }
        periodMap(period)
      }
    }
  }
}
