package com.azavea.opentransit.io

import com.azavea.gtfs.data.GtfsData
import com.azavea.gtfs.slick.DAO

import com.typesafe.config.ConfigFactory

import scala.slick.jdbc.{StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.{Database, Session, DatabaseDef}
import scala.slick.jdbc.meta.MTable

object GtfsIngest {
  private val dbGeomNameLatLng = {
    val config = ConfigFactory.load
    config.getString("database.geom-name-lat-lng")
  }

  /** Ingest the GTFS data into the database this session is tied to */
  def apply(gtfsDir: String)(implicit session: Session): Int = {
    val data = GtfsData.fromFile(gtfsDir)

    val dao = new DAO
    // data is read from the file and inserted into the db as lat/lng
    dao.geomColumnName = dbGeomNameLatLng

    // insert GTFS data into the database
    data.routes.foreach { route => dao.routes.insert(route) }
    data.service.foreach { service => dao.service.insert(service) }
    data.agencies.foreach { agency => dao.agencies.insert(agency) }
    data.trips.foreach { trip => dao.trips.insert(trip) }
    data.shapes.foreach { shape => dao.shapes.insert(shape) }
    data.stops.foreach { stop => dao.stops.insert(stop) }
    println("finished parsing GTFS data")

    println("Transforming GTFS to local UTM zone.")

    // Get the SRID of the UTM zone which contains the center of the GTFS data.  This is
    // done by finding the centroid of the bounding box of all GTFS stops, and then
    // doing a spatial query on utm_zone_boundaries to figure out which UTM zone
    // contains the centroid point. If a GTFS feed spans the boundary of two (or more)
    // UTM zones, it will be arbitrarily assigned to the zone into which the centroid
    // point falls. This isn't expected to significantly increase error.
    // Note: This assumes that the gtfs_stops table is in the same projection as the
    // utm_zone_boundaries table (currently 4326). If that ever changes then this query
    // will need to be updated.
    val sridQ = Q.queryNA[Int]("""SELECT srid FROM utm_zone_boundaries
        WHERE ST_Contains(utm_zone_boundaries.geom,
            (SELECT ST_SetSRID(ST_Centroid((SELECT ST_Extent(the_geom) from gtfs_stops)),
            Find_SRID('public', 'gtfs_stops', 'the_geom'))));
        """)
    val srid = sridQ.list.head

    geomTransform(srid, "gtfs_stops", "Point", "geom")
    geomCopy(srid, "gtfs_stops", "the_geom", "geom")
    geomTransform(srid, "gtfs_shape_geoms", "LineString", "geom")
    geomCopy(srid, "gtfs_shape_geoms", "the_geom", "geom")

    // Now that the SRID of the GTFS data is known, we also need to set the SRID
    // for imported shapefile data (boundaries and demographics).
    geomTransform(srid, "utm_datasources_boundary", "MultiPolygon", "geom")
    geomTransform(srid, "utm_datasources_demographicdatafeature", "MultiPolygon", "geom")

    println("Finished transforming to local UTM zone.")
    data.routes.size
  }


  // Reproject gtfs_stops.geom and gtfs_shape_geoms.geom to the UTM srid.
  // Directly interpolating into SQL query strings isn't best practice,
  // but since the value is pre-loaded into the database, it's safe and the simplest
  // thing to do in this case.
  private def geomTransform(srid: Int, table: String, geomType: String, column: String)(implicit session: Session) =
    // only alter the table if it exists
    if (!MTable.getTables(table).list.isEmpty) {
      (Q.u +
        s"ALTER TABLE ${table} ALTER COLUMN ${column} " +
        s"TYPE Geometry(${geomType},${srid}) " +
        s"USING ST_Transform(${column},${srid});").execute
    }


  private def geomCopy(srid: Int, table: String, fromColumn: String, toColumn: String)(implicit session: Session) =
    (Q.u +
      s"UPDATE ${table} SET ${toColumn} = ST_Transform(${fromColumn}, ${srid});").execute

}
