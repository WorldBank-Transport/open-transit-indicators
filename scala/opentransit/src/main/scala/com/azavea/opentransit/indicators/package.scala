package com.azavea.opentransit

import scala.collection.mutable

package object indicators {
  /** Takes a sequence of Map[Key, Value] and collects the values of the same keys
    * to give one Map[Key, Seq[Value]]
    */
  implicit class CombineMapsWrapper[TKey, TValue](maps: Seq[Map[TKey, TValue]]) {
    def combineMaps(): Map[TKey, Seq[TValue]] =
      maps.map(_.toSeq).flatten.groupBy(_._1).mapValues(_.map(_._2))
  }

  /** Takes a sequence of Map[Key, Seq[Value]] and merges the sequences of the same key
    * to give one Map[Key, Seq[Value]]
    */
  implicit class CombineSeqMapsWrapper[TKey, TValue](maps: Seq[Map[TKey, Seq[TValue]]]) {
    def combineMaps(): Map[TKey, Seq[TValue]] =
      maps.map(_.toSeq).flatten.groupBy(_._1).mapValues(_.map(_._2).flatten)
  }

  /** Takes a map of a key to a Tuple2 and separates it into two maps */
  implicit class SeparateTupleMapWrapper[TKey, TValue1, TValue2](map: Map[TKey, (TValue1, TValue2)]) {
    def separateMaps(): (Map[TKey, TValue1], Map[TKey, TValue2]) = {
      val m1 = mutable.Map[TKey, TValue1]()
      val m2 = mutable.Map[TKey, TValue2]()

      for((key, (v1, v2)) <- map) {
        m1(key) = v1
        m2(key) = v2
      }

      (m1.toMap, m2.toMap)
    }
  }

  /** Like the 'transpose' function on collections of collections, but with tuples.
    * Keeps the type information of the tuples. Only need it for Tuple3 for now, could add
    * more.
    * TODO: What would this look like with HLists in shapeless?
    */
  implicit class TuplesTransposeWrapper[T1, T2, T3](tuples: Seq[(T1, T2, T3)]) {
    def transposeTuples: (Seq[T1], Seq[T2], Seq[T3]) = {
      val l1 = mutable.ListBuffer[T1]()
      val l2 = mutable.ListBuffer[T2]()
      val l3 = mutable.ListBuffer[T3]()

      for((e1, e2, e3) <- tuples) {
        l1 += e1
        l2 += e2
        l3 += e3
      }

      (l1.toSeq, l2.toSeq, l3.toSeq)
    }

  }
}
