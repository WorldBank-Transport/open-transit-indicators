package geotrellis.network.graph

import org.scalatest._

class IntPriorityQueueSpec extends FunSpec
                              with ShouldMatchers {
  describe("IntPriorityQueue") {
    it("should dequeue in the correct order") {
      val weights = Array(6, 7, 4, 2, 9, 8, 10, 1, 20, 17, 14)
      val queue = new IntPriorityQueue(weights)
      for(i <- 0 until weights.size) {
        queue += i
      }

      val sorted = weights.zipWithIndex.sortBy(_._1).toArray
      for(i <- 0 until weights.size) {
        queue.dequeue should be (sorted(i)._2)
      }

      queue.isEmpty should be (true)

      queue += 0
      queue += 1
      queue += 2

      queue.dequeue should be (2)
      queue.dequeue should be (0)
      queue.dequeue should be (1)
      queue.isEmpty should be (true)
    }
  }
}
