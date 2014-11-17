package geotrellis.transit

import geotrellis.transit._
import geotrellis.transit.loader.GraphFileSet
import geotrellis.transit.loader.gtfs.{GtfsDateFiles}
import geotrellis.transit.loader.osm.OsmFileSet
import geotrellis.vector._

import geotrellis.network._
import geotrellis.network.graph._

import com.typesafe.config.{ConfigFactory, 
                            Config,
                            ConfigException}
import org.joda.time.LocalDate

import scala.collection.mutable
import scala.collection.JavaConversions._

import java.io._

object Configuration {
  private var _config:Configuration = null

  def get = {
    if(_config != null) { _config }
    else { sys.error("Configuration has not been loaded.") }
  }

  def loadPath(path:String) = {
    val json = ConfigFactory.parseFile(new File(path))

    val loaderConfig = () => {
      if(json.hasPath("loader")) {
        try {
          Some(LoaderConfiguration(json.getConfig("loader")))
        } catch {
          case e:ConfigException.WrongType =>
            sys.error("Configuration at $path has invalid loader section.")
        }
      } else { None }
    }

    val graphConfig = () => {
      if(json.hasPath("graph")) {
        try {
          Some(GraphConfiguration(json.getConfig("graph")))
        } catch {
          case e:ConfigException.WrongType =>
            sys.error("Configuration at $path has invalid graph section.")
        }
      } else { None } 
    }

    _config = new Configuration(path,loaderConfig,graphConfig)
    _config
  }
}

class Configuration(path:String,
                    loaderConfig:()=>Option[LoaderConfiguration], 
                    graphConfig:()=>Option[GraphConfiguration]) {
  lazy val loader =
    loaderConfig() match {
      case Some(lc) => lc
      case None => sys.error(s"Configuration at path $path does not contain loader information.")
    }

  lazy val graph =
    graphConfig() match {
      case Some(gc) => gc
      case None => sys.error(s"Configuration at path $path does not contain graph information.")
    }
}

class LoaderConfiguration(val fileSets:Seq[GraphFileSet])
object LoaderConfiguration { 
  def apply(json:Config) = {
    val fileSets = mutable.ListBuffer[GraphFileSet]()

    // GTFS
    try {
      if(json.hasPath("gtfs")) {
        for(gtfsJson <- json.getConfigList("gtfs")) {
          val name =
            if(gtfsJson.hasPath("name")) {
              gtfsJson.getString("name")
            } else { sys.error("Configuration error: Gtfs file loader entry needs a 'name' field.") }

          val dataPath =
            if(gtfsJson.hasPath("path")) {
              gtfsJson.getString("path")
            } else { sys.error("Configuration error: Gtfs file loader entry needs a 'path' field.") }
          fileSets += GtfsDateFiles(name,dataPath, new LocalDate(2013, 5 , 17))
        }
      }
    } catch {
      case e:ConfigException.WrongType =>
        sys.error("Configuration has invalid gtfs section.")
    }

    // OSM
    try {
      if(json.hasPath("osm")) {
        for(gtfsJson <- json.getConfigList("osm")) {
          val name =
            if(gtfsJson.hasPath("name")) {
              gtfsJson.getString("name")
            } else { sys.error("Configuration error: OSM file loader entry needs a 'name' field.") }

          val path =
            if(gtfsJson.hasPath("path")) {
              gtfsJson.getString("path")
            } else { sys.error("Configuration error: OSM file loader entry needs a 'path' field.") }

          fileSets += OsmFileSet(name,path)
        }
      }
    } catch {
      case e:ConfigException.WrongType =>
        sys.error("Configuration has invalid osm section.")
    }

    new LoaderConfiguration(fileSets.toSeq)
  }
}

object GraphConfiguration {
  def apply(json:Config) = {
    if(json.hasPath("data")) {
      val path = json.getString("data")
      val d = new File(path)
      if(!d.exists) {
        sys.error(s"Data directory $d does not exist.")
      }

      if(!d.isDirectory) {
        sys.error(s"Data directory path $d is not a directory")
      }

      new GraphConfiguration(d.getPath)
    } else { 
      sys.error("Configuration requires a 'data' field containing the path to the data directory.") 
    }
  }
}

class GraphConfiguration(val dataDirectory:String) {
  private def read[T](path:String) = {
    val input = new ObjectInputStream(new FileInputStream(path)) {
      override def resolveClass(desc: java.io.ObjectStreamClass): Class[_] = {
        try { Class.forName(desc.getName, false, getClass.getClassLoader) }
        catch { case ex: ClassNotFoundException => super.resolveClass(desc) }
      }
    }
    try {
      input.readObject().asInstanceOf[T]
    }
    finally{
      input.close()
    }
  }

  def getContext = {
    val files = new File(dataDirectory)
      .listFiles
      .map(_.getName)
      .toSeq

    // Validate directory
    if(!files.contains("transit.graph")) {
      sys.error(s"Data directory path $dataDirectory does not contain a 'transit.graph' graph data file.")
    }

    if(!files.contains("transit.vertices")) {
      sys.error(s"Data directory path $dataDirectory does not contain a 'transit.vertices' graph data file.")
    }

    if(!files.contains("transit.edges")) {
      sys.error(s"Data directory path $dataDirectory does not contain a 'transit.edges' graph data file.")
    }

    Logger.timedCreate("Reading graph file object...","Read graph object") { () =>
      val graph:TransitGraph = read(new File(dataDirectory, "transit.graph").getPath)
      val vertices:NamedLocations = read(new File(dataDirectory, "transit.vertices").getPath)
      val edges:NamedWays = read(new File(dataDirectory, "transit.edges").getPath)

      val index = createSpatialIndex(graph)

      GraphContext(graph,index,vertices,edges)
    }
  }

  def createSpatialIndex(graph:TransitGraph) = 
    Logger.timedCreate("Creating spatial index...", "Spatial index created.") { () =>
      SpatialIndex(0 until graph.vertexCount) { v => 
        val l = graph.location(v)
        (l.lat,l.long)
      }
    }
}
