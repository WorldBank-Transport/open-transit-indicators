package com.azavea.opentransit

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
}
