package com.azavea.gtfs

package object op {
  implicit class InterpolateStopTimeWrapper(records: GtfsRecords) {
    def interpolateStopTimes(): GtfsRecords = 
      InterpolateStopTimes(records)
  }
}
