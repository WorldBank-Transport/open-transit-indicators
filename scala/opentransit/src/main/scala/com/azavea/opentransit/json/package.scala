
package com.azavea.opentransit

import com.azavea.opentransit.indicators._
import com.azavea.opentransit.scenarios._
import com.azavea.opentransit.service._

import com.azavea.gtfs._

import scala.util.{Try, Success, Failure}
import spray.json._
import DefaultJsonProtocol._

import geotrellis.vector.json._

import com.github.nscala_time.time.Imports._
import org.joda.time.format.ISODateTimeFormat

object JobStatusType extends Enumeration {
  type JobStatusType = Value
  val Submitted = Value("queued")
  val Processing = Value("processing")
  val Complete = Value("complete")
  val Failed = Value("error")
}

import JobStatusType._

case class JobStatusWithMessage(statustype: JobStatusType, msg: String = "") {
  def equals(o: JobStatusWithMessage) = { statustype == o.statustype }
  override def hashCode = statustype.hashCode
  override def toString = statustype.toString
  def getJsonWithMsg = {
    JsObject(
      "status" -> JsString(statustype.toString),
      "msg" -> JsString(msg)
    )
  }
}

object JobStatus extends Enumeration {
  type JobStatus = JobStatusWithMessage
  val Submitted = JobStatusWithMessage(JobStatusType.Submitted)
  val Processing = JobStatusWithMessage(JobStatusType.Processing)
  val Complete = JobStatusWithMessage(JobStatusType.Complete)
  val Failed = JobStatusWithMessage(JobStatusType.Failed)
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
    // Converts a JsValue to a LocalDateTime, and if it fails, uses the default.
    // This error-handling is needed, because the alltime indicator comes across
    // with start/end times that the convertTo function cannot parse successfully.
    def toLocalDateTime(dt: JsValue): LocalDateTime = {
      Try(dt.convertTo[LocalDateTime]) match {
        case Success(v) => v
        case Failure(_) => new LocalDateTime
      }
    }

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
          val start = toLocalDateTime(startJson)
          val end = toLocalDateTime(endJson)
          SamplePeriod(id.toInt, periodType, start, end)
        case _ => throw new DeserializationException("SamplePeriod expected.")
      }
  }

  implicit object RequirementsJsonFormat extends RootJsonFormat[Requirements] {
    def write(r: Requirements): JsObject = JsObject(
      "demographics" -> JsBoolean(r.demographics),
      "osm" -> JsBoolean(r.osm),
      "observed" -> JsBoolean(r.observed),
      "city_bounds" -> JsBoolean(r.cityBounds),
      "region_bounds" -> JsBoolean(r.regionBounds),
      "job_demographics" -> JsBoolean(r.jobDemographics)
    )

    def read(v: JsValue): Requirements =
      v.asJsObject.getFields(
        "demographics",
        "osm",
        "observed",
        "city_bounds",
        "region_bounds"
      ) match {
        case Seq(JsBoolean(demographics), JsBoolean(osm), JsBoolean(observed),
                 JsBoolean(cityBounds), JsBoolean(regionBounds)) =>
          Requirements(demographics, osm, observed, cityBounds, regionBounds, true) // TODO: Read job_demographics from JSON after Django is sending it.
        case _ => throw new DeserializationException("IndicatorCalculationRequest expected.")
      }
  }

  // TODO: Remove TravelshedRequest
  implicit object IndicatorCalculationRequestFormat extends RootJsonReader[IndicatorCalculationRequest] {
    def read(value: JsValue): IndicatorCalculationRequest =
      value.asJsObject.getFields(
        "token",
        "id",
        "poverty_line",
        "nearby_buffer_distance_m",
        "arrive_by_time_s",
        "max_commute_time_s",
        "city_boundary_id",
        "region_boundary_id",
        "avg_fare",
        "gtfs_db_name",
        "aux_db_name",
        "sample_periods",
        "params_requirements"
      ) match {
        case Seq(JsString(token), JsNumber(id), JsNumber(povertyLine),
                 JsNumber(nearbyBufferDistance), JsNumber(arriveByTime), JsNumber(maxCommuteTime),
                 JsNumber(cityBoundaryId), JsNumber(regionBoundaryId),
                 JsNumber(averageFare), JsString(gtfsDbName), JsString(auxDbName),
                 samplePeriodsJson, paramsRequirementsJson) =>
          val samplePeriods = samplePeriodsJson.convertTo[List[SamplePeriod]]
          val paramsRequirements = paramsRequirementsJson.convertTo[Requirements]
          IndicatorCalculationRequest(
            token, id.toInt, povertyLine.toDouble, nearbyBufferDistance.toDouble,
            arriveByTime.toInt, maxCommuteTime.toInt, cityBoundaryId.toInt, regionBoundaryId.toInt,
            averageFare.toDouble, gtfsDbName, auxDbName, samplePeriods, paramsRequirements
          )
        case _ => throw new DeserializationException("IndicatorCalculationRequest expected.")
      }
  }

  implicit object ScenarioCreationRequestFormat extends RootJsonFormat[ScenarioCreationRequest] {
    def write(request: ScenarioCreationRequest) =
      JsObject(
        "token" -> JsString(request.token),
        "db_name" -> JsString(request.dbName),
        "base_db_name" -> JsString(request.baseDbName),
        "sample_period" -> request.samplePeriod.toJson
      )

    def read(value: JsValue): ScenarioCreationRequest =
      value.asJsObject.getFields(
        "token",
        "db_name",
        "base_db_name",
        "sample_period"
      ) match {
        case Seq(JsString(token), JsString(dbName), JsString(baseDbName), samplePeriodJson) =>
          val samplePeriod = samplePeriodJson.convertTo[SamplePeriod]
          ScenarioCreationRequest(token, dbName, baseDbName, samplePeriod)
        case _ => throw new DeserializationException("ScenarioCreationRequest expected.")
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

  implicit object IndicatorJobWriter extends RootJsonWriter[IndicatorJob] {
    def write(job: IndicatorJob) = {
      // A job is complete if nothing is processing or submitted
      val isComplete = job.status.map { case (period, indicatorResult) =>
        indicatorResult.forall { s =>
          s._2 != JobStatus.Processing && s._2 != JobStatus.Submitted
        }
      }.foldLeft(true)(_ && _)
      val jobStatus = if (isComplete)
        if (job.status.map { case (period, indicatorResult) =>
          indicatorResult.forall { s => s._2 != JobStatus.Failed }
        }.foldLeft(true)(_ && _)) JobStatus.Complete else JobStatus.Failed
        else JobStatus.Processing
      val calculationStatus = job.status.map { case (periodType, indicatorStatus) =>
        (periodType, indicatorStatus.map { case (indicatorName, status) =>
          (indicatorName, status.getJsonWithMsg)
        }.toMap)
      }.toMap

      JsObject(
        "id" -> JsNumber(job.id),
        "job_status" -> JsString(jobStatus.toString),
        "calculation_status" -> JsString(calculationStatus.toJson.toString)
      )
    }
  }

  implicit object ScenarioWriter extends RootJsonWriter[Scenario] {
    def write(scenario: Scenario) = {
      JsObject(
        "db_name" -> JsString(scenario.dbName),
        "job_status" -> JsString(scenario.jobStatus.toString)
      )
    }
  }

  implicit object GtfsFeedWriter extends RootJsonWriter[GtfsFeed] {
    def write(gtfsFeed: GtfsFeed) = {
      JsObject(
        "id" -> JsNumber(gtfsFeed.id),
        "status" -> JsString(gtfsFeed.jobStatus.toString)
      )
    }
  }
}
