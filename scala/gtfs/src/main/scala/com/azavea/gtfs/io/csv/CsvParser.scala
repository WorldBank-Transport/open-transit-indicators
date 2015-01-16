package com.azavea.gtfs.io.csv

import java.io.{FileInputStream, InputStreamReader, BufferedReader}
import java.nio.charset.Charset
import org.apache.commons.io.input._
import org.apache.commons.csv._

class CsvParser[T](
    private val reader:BufferedReader
  ) extends Iterator[String=>Option[String]] {

  val parser = new CSVParser(reader)
  
  val head = parser.getLine //this is the first line
  val idx = head.zip(0 until head.length).toMap

  var rec:Array[String] = null

  val getCol: String=>Option[String] = {name: String =>
    idx.get(name) match {
      case Some(index) => Some(rec(index))
      case None => None
    }
  }

  override def hasNext: Boolean = {
    rec = parser.getLine
    rec != null
  }

  override def next() = {
    getCol
  }
}

object CsvParser {
  def fromPath[T](path:String) = {
    new CsvParser(
      new BufferedReader( new InputStreamReader(
        new BOMInputStream(new FileInputStream(path), false), "UTF-8"))
    )
  }

}


