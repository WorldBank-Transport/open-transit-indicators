package geotrellis.transit

import com.vividsolutions.jts.index.strtree.{STRtree, ItemDistance, ItemBoundable}
import com.vividsolutions.jts.index.strtree.ItemDistance
import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.Envelope

import scala.collection.mutable
import scala.collection.JavaConversions._

import geotrellis.vector.Extent

// object SpatialIndex {
//   def apply(points:Iterable[(Double,Double)])
//            (implicit di:DummyImplicit):SpatialIndex[(Double,Double)] = {
//     val si = new SpatialIndex[(Double,Double)](Measure.Dumb)
//     for(point <- points) {
//       si.insert(point, point._1, point._2)
//     }
//     si
//   }

//   def apply[T](points:Iterable[T])(f:T=>(Double,Double)):SpatialIndex[T] = {
//     val si = new SpatialIndex[T](Measure.Dumb)
//     for(point <- points) {
//       val (x,y) = f(point)
//       si.insert(point, x, y)
//     }
//     si
//   }
// }

// class SpatialIndex[T](val measure:Measure) extends Serializable {
//   val rtree = new STRtree
//   val points = mutable.Set[T]()

//   def insert(v:T, x: Double, y: Double) = {
//     rtree.insert(new Envelope(new Coordinate(x,y)), v)
//     points.add(v)
//   }

//   def nearest(x:Double,y:Double):T = {
//     rtree.nearestNeighbour(new Envelope(new Coordinate(x,y)),null,measure).asInstanceOf[T]
//   }

//   def nearest(pt:(Double,Double)):T = {
//     val e = new Envelope(new Coordinate(pt._1,pt._2))
//     rtree.nearestNeighbour(e,null,measure).asInstanceOf[T]
//   }

//   // def nearestInExtent(extent:Extent,pt:(Double,Double)):Option[T] = {
//   //   val l = pointsInExtent(extent)
//   //   if(l.isEmpty) { None }
//   //   else {
//   //     var nearest = l.head
//   //     var minDist = {
//   //       val (x,y) = f(nearest)
//   //       measure.distance(x,y,pt._1,pt._2)
//   //     }
//   //     for(t <- l.tail) {
//   //       val (x,y) = f(t)
//   //       val d = measure.distance(pt._1,pt._2,x,y)
//   //       if(d < minDist) {
//   //         nearest = t
//   //         minDist = d
//   //       }
//   //     }
//   //     Some(nearest)
//   //   }
//   // }

//   def pointsInExtent(extent:Extent):Seq[T] = {
//     rtree.query(new Envelope(extent.ymin,extent.ymax,extent.xmin,extent.xmax))
//          .map(_.asInstanceOf[T])
//   }

//   def pointsInExtentAsJavaList(extent:Extent):List[_] = {
//     rtree.query(new Envelope(extent.ymin,extent.ymax,extent.xmin,extent.xmax)).toList
//   }
  
  
//   // def mergeIn(other:SpatialIndex[T]) = {
//   //   for(point <- other.points) { insert(point) }
//   //   this
//   // }
// }

// object Measure {
//   def Dumb = new DumbMeasure
// }

// trait Measure extends ItemDistance with Serializable {
//   def distance(x1:Double,y1:Double,x2:Double,y2:Double):Double

//   def distance(i1:ItemBoundable, i2:ItemBoundable):Double = {
//     val bound1 = i1.getBounds.asInstanceOf[Envelope]
//     val bound2 = i2.getBounds.asInstanceOf[Envelope]
//     distance(bound1.getMinX,bound1.getMinY,bound2.getMinX,bound2.getMinY)
//   }
// }

// class DumbMeasure() extends Measure {
//   def distance(x1:Double,y1:Double,x2:Double,y2:Double):Double = {
//     val x = x2 - x1
//     val y = y2 - y1
//     math.sqrt(x*x + y*y)
//   }
// }
