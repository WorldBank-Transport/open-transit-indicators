package com.azavea.gtfs.op

import com.azavea.gtfs._

/**
  * Expresses trips with frequency when they are repeated
  * @param trips
  * @param threshold Number of trips repated with predictable headway before compressing
  * @return
  */
object CompressTrips {
  def apply(records: GtfsRecords, threshold: Int = 2): GtfsRecords = {
    // TODO: Reimplement. This isn't being used anywhere so I'm not sweating it for now.
    ???

  // def compress(trips: Seq[TripRecord], threshold: Int = 2): Array[TripRecord] = {
  //  //println("Binning trips...")
  //  var compressed:Long = 0

  //  val bins = bin(trips)
  //  val ret = ArrayBuffer.empty[TripRecord]
  //   for (bin <- bins.map(_.sortBy(_.stopTimes.head.arrivalTime.millis))) {
  //     if (bin.length < threshold) {
  //       ret ++= bin
  //     }else{
  //       val headways =
  //       {
  //         for( Array(a,b) <- bin.sliding(2) )
  //         yield b.stopTimes.head.arrivalTime - a.stopTimes.head.arrivalTime
  //       }.toList

  //       for (rl <- RunLength((headways.head :: headways) zip bin){(c1,c2) => c1._1 == c2._1}) {
  //         rl match {
  //           case Run(1, List((headway, trip))) =>
  //             ret += trip
  //           case Run(x, list) =>
  //             val minTime = list.map(_._2.stopTimes.head.arrivalTime.millis).min
  //             val maxTime = list.map(_._2.stopTimes.head.arrivalTime.millis).max
  //             val model = list.head._2
  //             val headway = list.head._1
  //             val newTrip = model.copy(id = model.id + "+", frequency =
  //               Some(FrequencyRecord(model.id, Period.millis(minTime), Period.millis(maxTime), headway.toStandardDuration)))
  //             ret += newTrip

  //             compressed += list.length - 1
  //         }
  //       }
  //     }
  //   }
  //   //println("Total Compressed: " + compressed)
  //   ret.toArray
  // }

  }

  // private def bin(trips: Seq[TripRecord]): Array[Array[TripRecord]] = {
  //   val bins = ArrayBuffer(ArrayBuffer(trips.head))

  //   //go where you belong
  //   def belongs(t: TripRecord): Boolean = {
  //     for (b <- bins) {
  //       //sameishnes is transitive, so this works
  //       if (t sameish b.head) {
  //         b += t
  //         return true
  //       }
  //     }
  //     false
  //   }

  //   for (t <- trips.tail) {
  //     if (! belongs(t)) {
  //       bins += ArrayBuffer(t) //You're in a class of you own!
  //     }
  //   }

  //   bins.map(_.toArray).toArray
  // }

  // /**
  //   * In order to be sameish two trips need to:
  //   * - belong to same service
  //   * - belong to same route
  //   * - have the same stops
  //   * - same travel time between stops
  //   * - same time at each stop

  //   * In general the two trips should be re-creatable from one trip and frequency record.
  //   */
  // private def sameish(that: TripRecord): Boolean = {
  //   //This is a super-safe assumption, it is in fact possible to have two services that run on same days
  //   if (this.serviceId != that.serviceId) return false
  //   if (this.routeId != that.routeId) return false
  //   if (that.stopTimes.length != this.stopTimes.length) return false
  //   val l_start = this.stopTimes.head.arrivalTime
  //   val r_start = that.stopTimes.head.arrivalTime

  //   for ( (l,r) <- stopTimes zip that.stopTimes) {
  //     if (l.stopId != r.stopId) return false
  //     if (l.arrivalTime - l_start != r.arrivalTime - r_start) return false
  //     if (l.departureTime - l_start != r.departureTime - r_start) return false
  //   }
  //   true
  // }
}
