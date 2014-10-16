
package com.azavea.opentransit

import com.azavea.opentransit.indicators._
import com.azavea.opentransit.service._

import com.azavea.gtfs._

import spray.json._
import DefaultJsonProtocol._

import geotrellis.vector.json._

import com.github.nscala_time.time.Imports._
import org.joda.time.format.ISODateTimeFormat

object CalculationStatus extends Enumeration {
  type CalculationStatus = Value
  val Submitted = Value("queued")
  val Processing = Value("processing")
  val Complete = Value("complete")
  val Failed = Value("error")
}

package object json {
  // TODO: Find if we need the DateTime part of the string for this.
  // DateTime gets printed\parsed in format like 2014-09-24T16:59:06-04:00
  // LocalDateTime gets printed\parsed in format like 2014-09-24T16:59:06
  // DateTime.toLocalDateTime calls might modify the time, could introduce bugs if
  // server is across a time zone from the user, since all GTFS data is in LocalDateTime.
  // I'm not changing this now as I'm not sure how it's parsed on the other side of the pipe.
  implicit object LocalDateTimeFormat extends JsonFormat[LocalDateTime] {
    private val isoParser = ISODateTimeFormat.dateTimeNoMillis()
    def write(ldt: LocalDateTime) = JsString(isoParser.print(ldt.toDateTime))
    def read(value: JsValue) = value match {
      case JsString(s) => isoParser.parseDateTime(s).toLocalDateTime
      case _ => throw new DeserializationException(s"Error parsing DateTime: $value")
    }
  }

  implicit object SamplePeriodFormat extends RootJsonFormat[SamplePeriod] {
    def write(samplePeriod: SamplePeriod) =
      JsObject(
        "id" -> JsNumber(samplePeriod.id),
        "type" -> JsString(samplePeriod.periodType),
        "period_start" -> samplePeriod.start.toJson,
        "period_end" -> samplePeriod.end.toJson
      )

    def read(value: JsValue): SamplePeriod =
      value.asJsObject.getFields("id", "type", "period_start", "period_end") match {
        case Seq(JsNumber(id), JsString(periodType), startJson, endJson) =>
          val start = startJson.convertTo[LocalDateTime]
          val end = endJson.convertTo[LocalDateTime]
          SamplePeriod(id.toInt, periodType, start, end)
        case _ => throw new DeserializationException("SamplePeriod expected.")
      }
  }

  implicit object IndicatorCalculationRequestFormat extends RootJsonFormat[IndicatorCalculationRequest] {
    def write(request: IndicatorCalculationRequest) =
      JsObject(
        "token" -> JsString(request.token),
        "version" -> JsString(request.version),
        "poverty_line" -> JsNumber(request.povertyLine),
        "nearby_buffer_distance_m" -> JsNumber(request.nearbyBufferDistance),
        "max_commute_time_s" -> JsNumber(request.maxCommuteTime),
        "max_walk_time_s" -> JsNumber(request.maxWalkTime),
        "city_boundary_id" -> JsNumber(request.cityBoundaryId),
        "region_boundary_id" -> JsNumber(request.regionBoundaryId),
        "avg_fare" -> JsNumber(request.averageFare),
        "sample_periods" -> request.samplePeriods.toJson
      )

    def read(value: JsValue): IndicatorCalculationRequest =
      value.asJsObject.getFields(
        "token",
        "version",
        "poverty_line",
        "nearby_buffer_distance_m",
        "max_commute_time_s",
        "max_walk_time_s",
        "city_boundary_id",
        "region_boundary_id",
        "avg_fare",
        "sample_periods",
        "run_accessibility"
      ) match {
        case Seq(JsString(token), JsString(version), JsNumber(povertyLine), JsNumber(nearbyBufferDistance),
                 JsNumber(maxCommuteTime), JsNumber(maxWalkTime), JsNumber(cityBoundaryId), JsNumber(regionBoundaryId),
                 JsNumber(averageFare), samplePeriodsJson, JsBoolean(runAccessibility)) =>
          val samplePeriods = samplePeriodsJson.convertTo[List[SamplePeriod]]
          IndicatorCalculationRequest(
            token, version, povertyLine.toDouble, nearbyBufferDistance.toDouble,
            maxCommuteTime.toInt, maxWalkTime.toInt, cityBoundaryId.toInt, regionBoundaryId.toInt,
            averageFare.toDouble, samplePeriods, runAccessibility
          )
        case _ => throw new DeserializationException("IndicatorCalculationRequest expected.")
      }
  }

  implicit object RouteTypeFormat extends JsonFormat[RouteType] {
    def write(routeType: RouteType) =
      JsNumber(routeType.id)

    def read(value: JsValue): RouteType =
      value match {
        case JsNumber(id) => RouteType(id.toInt)
        case _ => throw new DeserializationException("RouteType is required to be an integer ID.")
      }
  }

  implicit object AggregateFormat extends JsonFormat[Aggregate] {
    def write(aggregate: Aggregate) =
      aggregate match {
        case RouteAggregate => JsString("route")
        case RouteTypeAggregate => JsString("mode")
        case SystemAggregate => JsString("system")
      }

    def read(value: JsValue): Aggregate =
      value match {
        case JsString(str) =>
          if(str == "route") RouteAggregate
          else if(str == "mode") RouteTypeAggregate
          else if(str == "system") SystemAggregate
          else { throw new DeserializationException(s"$str is not a recognized aggregation") }
        case _ =>
          throw new DeserializationException("Expected aggregation to be a string.")
      }
  }

  implicit object IndicatorResultContainerWriter extends RootJsonWriter[IndicatorResultContainer] {
    def write(container: IndicatorResultContainer) =
      JsObject(
        "type" -> JsString(container.indicatorId),
        "sample_period" -> JsString(container.samplePeriodType),
        "aggregation" -> container.aggregation.toJson,
        "value" -> JsNumber(container.value),
        "geom" -> container.geom.toJson,
        "version" -> JsString(container.version),
        "route_id" -> JsString(container.routeId),
        ("route_type",
          container.routeType match {
            case Some(routeType) => JsString(routeType.id.toString)
            case None => JsString("")
          }
        ),
        "city_bounded" -> JsBoolean(container.cityBounded)
      )
  }

  implicit object IndicatorJobWriter extends RootJsonWriter[IndicatorJob] {
    def write(job: IndicatorJob) = {
      // A job is complete if nothing is processing or submitted
      val isComplete = job.status.forall(s =>
        s._2 != CalculationStatus.Processing && s._2 != CalculationStatus.Submitted)
      val jobStatus = if (isComplete) (
        if (job.status.forall(s => s._2 != CalculationStatus.Failed)) CalculationStatus.Complete else CalculationStatus.Failed) 
        else CalculationStatus.Processing
      val calculationStatus = job.status.map { case(k, v) => (k, v.toString)}.toMap

      JsObject(
        "version" -> JsString(job.version),
        "job_status" -> JsString(jobStatus.toString),
        "calculation_status" -> JsString(calculationStatus.toJson.toString)
      )
    }
  }
}
