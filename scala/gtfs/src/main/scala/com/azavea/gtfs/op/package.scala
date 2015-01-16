package com.azavea.gtfs

import org.joda.time.LocalDateTime

package object op {
  implicit class InterpolateStopTimeWrapper(records: GtfsRecords) {
    def interpolateStopTimes(): GtfsRecords = 
      InterpolateStopTimes(records)

    def filter(start: LocalDateTime, end: LocalDateTime) =
      TimeFilter(records, start, end)
  }
}
