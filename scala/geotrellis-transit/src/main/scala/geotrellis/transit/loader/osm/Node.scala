package geotrellis.transit.loader.osm

import scala.collection.mutable

case class Node(id:String,lat:Double,long:Double) {
  val outgoingEdges = mutable.ListBuffer[Node]()

  override
  def hashCode = id.hashCode

  override 
  def equals(other: Any) = 
    other match { 
      case that: Node => this.id == that.id
      case _ => false
    }

  def addEdgeTo(node:Node) =
    outgoingEdges += node
}
