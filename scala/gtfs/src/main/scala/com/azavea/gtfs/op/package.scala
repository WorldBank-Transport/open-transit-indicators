package com.azavea.gtfs

package object op {
  implicit class InterpolateStopTimeWrapper(records: GtfsRecords) {
    def interpolateStopTimes(): GtfsRecords = 
      InterpolateStopTimes(records)
  }

  /**
    * Expresses trips with frequency when they are repeated
    * @param trips
    * @param threshold Number of trips repated with predictable headway before compressing
    * @return
    */
  implicit class CompressTripsWrapper(records: GtfsRecords) {
    def compressTrips(threshold: Int = 2): GtfsRecords =
      CompressTrips(records)
  }
}
