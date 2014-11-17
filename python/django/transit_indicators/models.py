# coding=UTF-8
import csv
from datetime import datetime
import uuid

from django.conf import settings
from django.contrib.gis.db import models
from django.db import transaction
from django.utils.translation import ugettext_lazy as _

from transit_indicators.gtfs import GTFSRouteTypes
from datasources.models import Boundary, DemographicDataFieldName, DemographicDataSource
from userdata.models import OTIUser


class GTFSRouteType(models.Model):
    """ Static definition of the gtfs route types

    This model is populated by a data migration using the
    gtfs.GTFSRouteTypes.CHOICES tuple

    """
    # Integer route type, is unique
    route_type = models.IntegerField(unique=True, choices=GTFSRouteTypes.CHOICES)

    class Meta(object):
        db_table = 'gtfs_route_types'

class OTICityName(models.Model):
    """ User-configurable name for their city; set with GTFS upload.
        There should only be a single entry in this table at any time.
    """
    city_name = models.CharField(blank=False, max_length=255, primary_key=True)

    def __unicode__(self):
        return self.city_name


class GTFSRoute(models.Model):
    route_id = models.TextField(primary_key=True)
    agency_id = models.TextField(blank=True)
    route_short_name = models.TextField(blank=True)
    route_long_name = models.TextField(blank=True)
    route_desc = models.TextField(blank=True)
    route_type = models.IntegerField(blank=True, null=True)
    route_url = models.TextField(blank=True)
    route_color = models.TextField(blank=True)
    route_text_color = models.TextField(blank=True)

    class Meta:
        managed = False
        db_table = 'gtfs_routes'


class OTIIndicatorsConfig(models.Model):
    """ Global configuration for indicator calculation. """
    # Suffixes denote database units; the UI may convert to other units for
    # display to users.
    # The local poverty line (in local currency, therefore no database units).
    poverty_line = models.FloatField()

    # The average fare (local currency, no database units)
    avg_fare = models.FloatField()

    # Buffer distance (meters) for indicators attempting to capture a concept of
    # nearness. E.g. "Percentage of elderly people living within X meters of a
    # bus stop."
    nearby_buffer_distance_m = models.FloatField()

    # The time to arrive to a location by for use in calculating travelsheds.
    # This is in 'seconds from midnight'.
    arrive_by_time_s = models.PositiveIntegerField()

    # The maximum allowable commute time (seconds). Used by the job accessibility
    # indicator to generate a travelshed for access to designated job
    # locations.
    max_commute_time_s = models.PositiveIntegerField()

    # TODO: geotrellis-transit does not support max durations by specific mode.
    # Maximum allowable walk time (seconds). Also used by the job accessibility indicator
    # when generating its travelshed.
    max_walk_time_s = models.PositiveIntegerField()

    # Setting related_name to '+' prevents Django from creating the reverse
    # relationship, which prevents a conflict because otherwise each Boundary
    # would have two fields named 'otiindicatorsconfig_set'. If we wanted, we
    # could name them something else to preserve both reverse relationships.
    # Set key to null when referenced boundary is deleted.
    # Boundary denoting the city -- used for calculating percentage of system
    # falling inside city limits
    city_boundary = models.ForeignKey(Boundary, blank=True, null=True,
                                      on_delete=models.SET_NULL, related_name='+')

    # Boundary denoting the region
    # Set key to null when referenced boundary is deleted.
    region_boundary = models.ForeignKey(Boundary, blank=True, null=True,
                                        on_delete=models.SET_NULL, related_name='+')


class OTIDemographicConfig(models.Model):
    """Stores configuration relating to demographic data.

    When POSTing a JSON representation of this object to
    /demographics/<datasource-id>/load/ to load a new set of data and generate a new
    configuration, send field _names_ rather than the id of a DemographicDataFieldName
    object; the serializer will take care of looking up the proper objects.
    E.g. {"pop_metric_1_label": "DIST_POP" }.
    """
    # Labels and field names for demographic metrics, of which there will
    # always be precisely three (3): Two (2) population metrics and one (1)
    # destination metric.
    # The label is human-readable and designed to be displayed to users.
    # The field is for accessing the data from the associated shapefile.
    pop_metric_1_label = models.CharField(max_length=255, blank=True, null=True)
    pop_metric_1_field = models.ForeignKey(DemographicDataFieldName, blank=True,
                                           null=True, related_name='+')

    pop_metric_2_label = models.CharField(max_length=255, blank=True, null=True)
    pop_metric_2_field = models.ForeignKey(DemographicDataFieldName, blank=True,
                                           null=True, related_name='+')

    dest_metric_1_label = models.CharField(max_length=255, blank=True, null=True)
    dest_metric_1_field = models.ForeignKey(DemographicDataFieldName, blank=True,
                                            null=True, related_name='+')

    # The datasource from which these data points will come
    datasource = models.ForeignKey(DemographicDataSource)


class SamplePeriod(models.Model):
    """Stores configuration for a slice of time that is used to calculate indicators.

    A sample period is a period of time within a specific date (or potentially two
    subsequent dates if crossing midnight). There are five sample period types, three
    of which are specified by the user (morning rush, evening rush, weekend), and two
    of which are inferred by filling in the gaps between those (mid day, night).
    There is also a special sample period: alltime that is used for all-time aggregation.
    """

    class SamplePeriodTypes(object):
        ALLTIME = 'alltime'
        MORNING = 'morning'
        MIDDAY = 'midday'
        EVENING = 'evening'
        NIGHT = 'night'
        WEEKEND = 'weekend'
        CHOICES = (
            (ALLTIME, _(u'All Time')),
            (MORNING, _(u'Morning Rush')),
            (MIDDAY, _(u'Mid Day')),
            (EVENING, _(u'Evening Rush')),
            (NIGHT, _(u'Night')),
            (WEEKEND, _(u'Weekend')),
        )
    type = models.CharField(max_length=7, choices=SamplePeriodTypes.CHOICES, unique=True)

    # Starting datetime of sample
    period_start = models.DateTimeField()

    # Ending datetime of sample
    period_end = models.DateTimeField()


class Scenario(models.Model):
    """Stores metadata about a scenario"""

    class StatusChoices(object):
        QUEUED = 'queued'
        PROCESSING = 'processing'
        ERROR = 'error'
        COMPLETE = 'complete'
        CHOICES = (
            (QUEUED, _(u'Scenario initialization queued for processing')),
            (PROCESSING, _(u'Scenario initialization is processing')),
            (ERROR, _(u'Error initializing scenario')),
            (COMPLETE, _(u'Completed scenario initialization')),
        )

    # Name of the database where the scenario is stored
    db_name = models.CharField(max_length=40, unique=True, default=uuid.uuid4)

    # Optional scenario to base this scenario off of
    base_scenario = models.ForeignKey('self', blank=True, null=True)

    create_date = models.DateTimeField(auto_now_add=True, default=datetime.now)
    last_modify_date = models.DateTimeField(auto_now=True, default=datetime.now)
    job_status = models.CharField(max_length=10, choices=StatusChoices.CHOICES)
    sample_period = models.ForeignKey(SamplePeriod)
    created_by = models.ForeignKey(OTIUser)
    name = models.CharField(max_length=50)
    description = models.CharField(max_length=255, blank=True, null=True)


""" Helper method to find the city name set for the database.
"""
def get_city_name():
    try:
        city_name = OTICityName.objects.get().city_name
    except OTICityName.DoesNotExist:
        city_name = settings.OTI_CITY_NAME
    return city_name


class IndicatorJob(models.Model):
    """Stores processing status of an indicator job"""

    class StatusChoices(object):
        QUEUED = 'queued'
        PROCESSING = 'processing'
        ERROR = 'error'
        TIMEDOUT = 'timedout'
        COMPLETE = 'complete'
        CHOICES = (
            (QUEUED, _(u'Job queued for processing')),
            (PROCESSING, _(u'Indicators being processed and calculated')),
            (ERROR, _(u'Error calculating indicators')),
            (COMPLETE, _(u'Completed indicator calculation')),
        )

    job_status = models.CharField(max_length=10, choices=StatusChoices.CHOICES)
    created_by = models.ForeignKey(OTIUser)

    # Optional scenario to calculate indicators for.
    # If a scenario isn't provided, the base data will be used.
    scenario = models.ForeignKey(Scenario, blank=True, null=True)

    # A city name used to differentiate indicator sets
    # external imports must provide a city name as part of the upload
    city_name = models.CharField(max_length=255, default=get_city_name)

    # json string map of indicator name to status
    calculation_status = models.TextField(default='{}')


class Indicator(models.Model):
    """Stores a single indicator calculation"""

    field_names = ['aggregation', 'city_bounded', 'city_name', 'id', 'route_id', 'route_type',
                   'sample_period', 'type', 'value', 'calculation_job']

    class LoadStatus(object):
        """Stores status of the load class method"""

        def __init__(self):
            """ Init - Defined so that these properties always exist for the __dict__ method"""
            self.success = False
            self.count = 0
            self.errors = []

    @classmethod
    def load(cls, data, city_name, user):
        """ Load passed csv into Indicator table using city_name as a reference value

        :param data: File object holding csv data with headers in Indicator.field_names
        :param city_name: Inserted into the IndicatorJob.city_name field, is a human-readable
                          string which denotes which dataset the indicator is attached to
        :param user: User loading this csv

        :return LoadStatus

        """
        response = cls.LoadStatus()
        sample_period_cache = {}
        if not city_name:
            response.errors.append('city_name parameter required')
            response.success = False
            return response
        try:
            import_job = IndicatorJob.objects.create(job_status=IndicatorJob.StatusChoices.PROCESSING,
                                      created_by=user, city_name=city_name)
            num_saved = 0
            dict_reader = csv.DictReader(data, fieldnames=cls.field_names)
            dict_reader.next()
            with transaction.atomic():
                for row in dict_reader:
                    # check that the exported city name matches the one the user specified
                    # (ignore any other cities in the uploaded CSV)
                    this_city = row.pop('city_name')
                    if this_city != city_name:
                        continue
                    sp_type = row.pop('sample_period', None)
                    calculation_job = row.pop('version', None)
                    if not sp_type:
                        continue
                    sample_period = sample_period_cache.get(sp_type, None)
                    if not sample_period:
                        sample_period = SamplePeriod.objects.get(type=sp_type)
                        sample_period_cache[sp_type] = sample_period
                    # autonumber ID field (do not use imported ID)
                    row.pop('id')
                    value = float(row.pop('value'))
                    # We are going to ignore the csv's calculation_job field because that depends
                    # upon the database in which the indicators are originally stored rather than
                    # the database that we're now uploading to
                    row.pop('calculation_job')
                    indicator = cls(sample_period=sample_period, calculation_job=import_job, value=value, **row)
                    indicator.save()
                    num_saved += 1
                response.count = num_saved
                response.success = True
                import_job.job_status = IndicatorJob.StatusChoices.COMPLETE
                import_job.save()

        except Exception as e:
            response.success = False
            response.errors.append(str(e))
            import_job.job_status = IndicatorJob.StatusChoices.ERROR
            import_job.save()

        return response

    class AggregationTypes(object):
        ROUTE = 'route'
        MODE = 'mode'
        SYSTEM = 'system'
        CHOICES = (
            (ROUTE, _(u'Route')),
            (MODE, _(u'Mode')),
            (SYSTEM, _(u'System')),
        )

    class IndicatorTypes(object):
        ACCESS_INDEX = 'access_index'
        AFFORDABILITY = 'affordability'
        AVG_SERVICE_FREQ = 'avg_service_freq'
        COVERAGE_STOPS = 'coverage_ratio_stops_buffer'
        DISTANCE_STOPS = 'distance_stops'
        DWELL_TIME = 'dwell_time'
        HOURS_SERVICE = 'hours_service'
        JOB_ACCESS = 'job_access'
        LENGTH = 'length'
        LINES_ROADS = 'lines_roads'
        LINE_NETWORK_DENSITY = 'line_network_density'
        NUM_MODES = 'num_modes'
        NUM_ROUTES = 'num_routes'
        NUM_STOPS = 'num_stops'
        NUM_TYPES = 'num_types'
        ON_TIME_PERF = 'on_time_perf'
        REGULARITY_HEADWAYS = 'regularity_headways'
        SERVICE_FREQ_WEIGHTED = 'service_freq_weighted'
        SERVICE_FREQ_WEIGHTED_LOW = 'service_freq_weighted_low'
        STOPS_ROUTE_LENGTH = 'stops_route_length'
        SUBURBAN_LINES = 'ratio_suburban_lines'
        SYSTEM_ACCESS = 'system_access'
        SYSTEM_ACCESS_LOW = 'system_access_low'
        TIME_TRAVELED_STOPS = 'time_traveled_stops'
        TRAVEL_TIME = 'travel_time'
        WEEKDAY_END_FREQ = 'weekday_end_freq'
        JOBS_TRAVELSHED = 'jobs_travelshed'

        class Units(object):
            AVG_DWELL_DEVIATION = _(u'avg deviation from scheduled dwell time')
            AVG_FREQ_DEVIATION = _(u'avg deviation from scheduled frequency')
            AVG_SCHEDULE_DEVIATION = _(u'avg deviation from scheduled time')
            SECONDS = _(u'secs')
            MINUTES = _(u'mins')
            HOURS = _(u'hrs')
            KILOMETERS = _(u'km')
            KM_PER_AREA = _(u'transit length/km²')
            LOW_INCOME_POP_PER_STOP = _(u'low-income pop served by stop')
            POP_PER_STOP = _(u'percent population served by a stop')
            PERCENT_STOP_COVERAGE = _(u'percent stop coverage')
            STOPS_PER_ROUTE_LENGTH = _(u'stops/km')

        # units of measurement for the IndicatorTypes
        INDICATOR_UNITS = {
                            AVG_SERVICE_FREQ: Units.MINUTES,
                            COVERAGE_STOPS: Units.PERCENT_STOP_COVERAGE,
                            DISTANCE_STOPS: Units.KILOMETERS,
                            DWELL_TIME: Units.AVG_DWELL_DEVIATION,
                            HOURS_SERVICE: Units.HOURS,
                            LENGTH: Units.KILOMETERS,
                            LINE_NETWORK_DENSITY: Units.KM_PER_AREA,
                            ON_TIME_PERF: Units.AVG_SCHEDULE_DEVIATION,
                            REGULARITY_HEADWAYS: Units.AVG_FREQ_DEVIATION,
                            SERVICE_FREQ_WEIGHTED: Units.MINUTES,
                            SERVICE_FREQ_WEIGHTED_LOW: Units.MINUTES,
                            STOPS_ROUTE_LENGTH: Units.STOPS_PER_ROUTE_LENGTH,
                            SYSTEM_ACCESS: Units.POP_PER_STOP,
                            SYSTEM_ACCESS_LOW: Units.LOW_INCOME_POP_PER_STOP,
                            TIME_TRAVELED_STOPS: Units.MINUTES,
                            TRAVEL_TIME: Units.MINUTES,
                            TIME_TRAVELED_STOPS: Units.MINUTES,
                            WEEKDAY_END_FREQ: Units.MINUTES,
                            JOBS_TRAVELSHED: Units.SECONDS
        }

        # indicators to display on the map
        INDICATORS_TO_MAP = frozenset([
                              LENGTH,
                              NUM_STOPS,
                              HOURS_SERVICE,
                              STOPS_ROUTE_LENGTH,
                              DISTANCE_STOPS,
                              TIME_TRAVELED_STOPS,
                              WEEKDAY_END_FREQ,
                              ON_TIME_PERF,
                              REGULARITY_HEADWAYS,
                              COVERAGE_STOPS
        ])

        CHOICES = (
            (ACCESS_INDEX, _(u'Access index')),
            (AFFORDABILITY, _(u'Affordability')),
            (AVG_SERVICE_FREQ, _(u'Average Service Frequency')),
            (COVERAGE_STOPS, _(u'Coverage of transit stops')),
            (DISTANCE_STOPS, _(u'Distance between stops')),
            (DWELL_TIME, _(u'Dwell Time Performance')),
            (HOURS_SERVICE, _(u'Weekly number of hours of service')),
            (JOB_ACCESS, _(u'Job accessibility')),
            (LENGTH, _(u'Transit system length')),
            (LINES_ROADS, _(u'Ratio of transit lines length over road length')),
            (LINE_NETWORK_DENSITY, _(u'Transit line network density')),
            (NUM_MODES, _(u'Number of modes')),
            (NUM_ROUTES, _(u'Number of routes')),
            (NUM_STOPS, _(u'Number of stops')),
            (NUM_TYPES, _(u'Number of route types')),
            (ON_TIME_PERF, _(u'On-Time Performance')),
            (REGULARITY_HEADWAYS, _(u'Regularity of Headways')),
            (SERVICE_FREQ_WEIGHTED, _(u'Service frequency weighted by served population')),
            (SERVICE_FREQ_WEIGHTED_LOW, _(u'Service frequency weighted by served low-income population')),
            (STOPS_ROUTE_LENGTH, _(u'Ratio of number of stops to route-length')),
            (SUBURBAN_LINES, _(u'Ratio of the Transit-Pattern Operating Suburban Lines')),
            (SYSTEM_ACCESS, _(u'System accessibility')),
            (SYSTEM_ACCESS_LOW, _(u'System accessibility - low-income')),
            (TIME_TRAVELED_STOPS, _(u'Time traveled between stops')),
            (TRAVEL_TIME, _(u'Travel Time Performance')),
            (WEEKDAY_END_FREQ, _(u'Weekday / weekend frequency')),
            (JOBS_TRAVELSHED, _(u'Number of jobs that can be reached by an area.'))
        )

    # Slice of time used for calculating this indicator
    sample_period = models.ForeignKey(SamplePeriod)

    # Type of indicator
    type = models.CharField(max_length=32, choices=IndicatorTypes.CHOICES)

    # Level in which this indicator is aggregated
    aggregation = models.CharField(max_length=6, choices=AggregationTypes.CHOICES)

    # Reference to the route id. Only relevant for a ROUTE aggregation type.
    # This is the route_id column found within the GTFS routes table.
    route_id = models.CharField(max_length=32, null=True)

    # Reference to the route type id. Only relevant for a MODE aggregation type.
    # This is the route_type column found within the GTFS routes table.
    route_type = models.PositiveIntegerField(null=True)

    # Whether or not this calculation is contained within the defined city boundaries
    city_bounded = models.BooleanField(default=False)

    # The job that calculated this indicator
    calculation_job = models.ForeignKey(IndicatorJob)

    # Numerical value of the indicator calculation
    value = models.FloatField(default=0)

    # Cached geometry for this indicator only used by Windshaft
    the_geom = models.GeometryField(srid=4326, null=True)

    objects = models.GeoManager()

    class Meta(object):
        # An indicators uniqueness is determined by all of these things together
        # Note that route_id and route_type can be null.
        unique_together = (("sample_period", "type", "aggregation", "route_id", "route_type", "calculation_job"),)
