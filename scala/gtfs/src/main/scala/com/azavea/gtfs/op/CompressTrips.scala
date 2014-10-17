package com.azavea.gtfs.op

import com.azavea.gtfs._

import scala.collection.mutable.ArrayBuffer
import com.github.nscala_time.time.Imports._

object GroupTrips {
  case class TripTuple(trip:TripRecord, stopTimes: Seq[StopTimeRecord]) {
    def toOffset = {
      val offset = stopTimes.head.arrivalTime
      val offsetTimes = stopTimes map { st =>
        st.copy(arrivalTime = st.arrivalTime - offset, departureTime = st.departureTime - offset)
      }
      OffsetTripTuple(trip, offset, offsetTimes)
    }
  }

  case class OffsetTripTuple(trip: TripRecord, offset: Period, stopTimes: Seq[StopTimeRecord]) {
    require(stopTimes.head.arrivalTime.toStandardSeconds.getSeconds  == 0)
    def mergeOffset = {
      val times = stopTimes map { st =>
        st.copy(arrivalTime = st.arrivalTime + offset, departureTime = st.departureTime + offset)
      }

      TripTuple(trip, times)
    }
  }

  def samePath(a: OffsetTripTuple, b: OffsetTripTuple): Boolean = {
    for ((aStop,bStop) <- a.stopTimes zip b.stopTimes) {
      if (aStop.stopId != bStop.stopId)
        return false

      // TODO there can be a concept of a margin here, within 1s
      if (aStop.arrivalTime   != bStop.arrivalTime)
        return false
      if (aStop.departureTime != bStop.departureTime)
        return false
    }
    true
  }

  def groupByPath(trips: Seq[TripTuple]): Array[Array[TripTuple]] = {
    val offsetTrips = trips.map(_.toOffset)
    val bins = ArrayBuffer(ArrayBuffer(offsetTrips.head))

    //go where you belong
    def belongs(x: OffsetTripTuple): Boolean = {
      for (bin <- bins) {
        if (samePath(x, bin.head)) {
          bin += x
          return true
        }
      }
      false
    }

    for (t <- offsetTrips.tail) {
      if (! belongs(t))
        bins += ArrayBuffer(t) //You're in a class of you own!
    }

    bins.map(_.map(_.mergeOffset).toArray).toArray
  }
}
