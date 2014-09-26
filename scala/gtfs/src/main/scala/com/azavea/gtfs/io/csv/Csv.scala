package com.azavea.gtfs.io.csv

import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.Charset

import scala.collection.mutable

object Csv {
  def fromPath(path:String) = {
    new Csv(
      new BufferedReader(
        new InputStreamReader(
          new FileInputStream(path),
          Charset.forName("UTF-8"))))
  }
}

/**
 * Class for reading GTFS csv data. Assumes headers. Not meant to be a generic CSV parser.
 */
class Csv(private val reader:BufferedReader) extends Iterator[Map[String,String]] {
  private val queue = mutable.Queue[Map[String,String]]()

  private var isSpent = false

  // Read headers.
  val headers = parseValues(reader.readLine)

  def hasNext = {
    if(isSpent) { false }
    else {
      if(!queue.isEmpty) { true }
      else {
        val nextLine = reader.readLine
        if(nextLine == null) {
          reader.close
          isSpent = true
          false
        } else {
          queue += parse(nextLine)
          true
        }
      }
    }
  }

  def next():Map[String,String] = {
    if(queue.isEmpty) {
      parse(reader.readLine)
    } else {
      queue.dequeue
    }
  }

  def parse(line:String):Map[String,String] = {
    headers.zip(parseValues(line)).toMap
  }

  def parseValues(line:String) = {

    line.split(',')
      .map(unquote(_).trim
      .replace("\"\"", "\""))
  }

  /**
   * If rec is surrounded by quotes it return the content between the quotes
   */
  def unquote(str: String) = {
    if (str != null && str.length >= 2 && str.charAt(0) == '\"' && str.charAt(str.length - 1) == '\"')
      str.substring(1, str.length - 1)
    else
      str
  }
} 
