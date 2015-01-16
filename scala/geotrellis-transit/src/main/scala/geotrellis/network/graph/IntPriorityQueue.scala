package geotrellis.network.graph

// Modified from Scala source code (2.11)

import scala.collection.mutable._

class IntPriorityQueue(weights: Array[Int]) {
  private val arr = Array.ofDim[Int](weights.size + 2)
  private var queueSize = 1

  def size = queueSize

  private def fixUp(m: Int): Unit = {
    var k: Int = m
    while (k > 1 && weights(arr(k / 2)) > weights(arr(k))) {
      val k2 = k / 2
      // swap
      val h = arr(k)
      arr(k) = arr(k2)
      arr(k2) = h
      k = k2
    }
  }

  private def fixDown(m: Int, n: Int): Unit = {
    var k: Int = m
    while (n >= 2 * k) {
      var j = 2 * k
      if (j < n && weights(arr(j)) > weights(arr(j + 1)))
        j += 1
      if (weights(arr(k)) <= weights(arr(j)))
        return
      else {
        // swap
        val h = arr(k)
        arr(k) = arr(j)
        arr(j) = h
        k = j
      }
    }
  }

  def isEmpty: Boolean = queueSize < 2

  /** Inserts a single element into the priority queue.
   *
   *  @param  elem        the element to insert.
   *  @return             this $coll.
   */
  def +=(elem: Int): Unit = {
    arr(queueSize) = elem
    fixUp(queueSize)
    queueSize += 1
  }


  /** Returns the element with the highest priority in the queue,
   *  and removes this element from the queue.
   *
   *  @throws Predef.NoSuchElementException
   *  @return   the element with the highest priority.
   */
  def dequeue(): Int =
    if (queueSize > 1) {
      queueSize = queueSize - 1

      val h = arr(1)
      arr(1) = arr(queueSize)
      arr(queueSize) = h

      fixDown(1, queueSize - 1)
      arr(queueSize)

    } else
      throw new NoSuchElementException("no element to remove from heap")

  override
  def toString: String = arr.toSeq.toString
}
