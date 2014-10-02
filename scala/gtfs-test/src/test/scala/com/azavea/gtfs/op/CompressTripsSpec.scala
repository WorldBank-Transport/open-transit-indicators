package com.azavea.gtfs

import com.github.nscala_time.time.Imports._
import org.scalatest._

class CompressTripsSpec extends FlatSpec with Matchers {
// TODO: Implement these tests whenever we need to implement CompressTrips

//   val trip1 = Trip("T1","SR1","R1",None,
//     List(
//       StopTime("S1","T1", 1, 0.seconds, 1.minute),
//       StopTime("S2","T1", 2, 10.minutes, 11.minutes),
//       StopTime("S3","T1", 3, 15.minutes, 16.minutes)
//     )
//   )
//   val trip2 = Trip("T2","SR1","R1",None,
//     List(
//       StopTime("S1","T2", 1, 1.minute + 0.seconds, 1.minute + 1.minute),
//       StopTime("S2","T2", 1, 1.minute + 10.minutes, 1.minute + 11.minutes),
//       StopTime("S3","T2", 1, 1.minute + 15.minutes, 1.minute + 16.minutes)
//     )
//   )
//   val trip3 = Trip("T3","SR1","R1",None,
//     List(
//       StopTime("S1","T3", 1, 1.minute + 0.seconds, 1.minute + 1.minute),
//       StopTime("S2","T3", 1, 1.minute + 10.minutes, 1.hour + 11.minutes), //long break here
//       StopTime("S3","T3", 1, 1.hour + 15.minutes, 1.hour + 16.minutes)
//     )
//   )

//   it should "recognize a shameish trip" in {
//     assert(trip1 sameish trip2, "Two trips that differ only in time offset are sameish")
//     assert(! (trip1 sameish trip3), "Two trips that differ in stop intervals are not sameish")
//   }

//   it should "know how to segregate" in {
//     val bins = Trip.bin(trip1 :: trip2 :: trip3 :: Nil)
//     val big = bins.find(_.size == 2).get
//     val small = bins.find(_.size == 1).get

//     big should contain (trip1)
//     big should contain (trip2)
//     small should contain (trip3)
//   }

}
