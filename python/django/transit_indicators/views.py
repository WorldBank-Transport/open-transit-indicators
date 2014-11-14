import django_filters
from datetime import datetime, time
import pytz

from django.conf import settings
from django.db import connection, ProgrammingError

from rest_framework import status
from rest_framework.filters import OrderingFilter
from rest_framework.response import Response
from rest_framework.settings import api_settings
from rest_framework.views import APIView
from rest_framework_csv.renderers import CSVRenderer

from viewsets import OTIBaseViewSet, OTIAdminViewSet
from models import (OTIIndicatorsConfig,
                    OTIDemographicConfig,
                    OTICityName,
                    SamplePeriod,
                    Indicator,
                    IndicatorJob,
                    Scenario,
                    GTFSRouteType,
                    GTFSRoute)
from transit_indicators.tasks import start_indicator_calculation, start_scenario_creation
from serializers import (OTIIndicatorsConfigSerializer, OTIDemographicConfigSerializer,
                         SamplePeriodSerializer, IndicatorSerializer, IndicatorJobSerializer,
                         ScenarioSerializer, OTICityNameSerializer)


class OTICityNameView(APIView):
    """ Endpoint to GET or POST the user-configured city name.
    """
    model = OTICityName
    serializer_class = OTICityNameSerializer

    def get(self, request, *args, **kwargs):
        try:
            city_name = OTICityName.objects.get().city_name
        except OTICityName.DoesNotExist:
            city_name = settings.OTI_CITY_NAME
        except OTICityName.MultipleObjectsReturned:
            return Response({'error': 'There cannot be multiple city names set!'}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
        return Response({'city_name': city_name}, status=status.HTTP_200_OK)

    def post(self, request, *args, **kwargs):
        serializer = OTICityNameSerializer(data=request.DATA)
        if serializer.is_valid():
            OTICityName.objects.all().delete() # delete any existing city names first
            serializer.save() # save the new one
            return Response(serializer.data, status=status.HTTP_201_CREATED)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


class OTIIndicatorsConfigViewSet(OTIAdminViewSet):
    """ Viewset for OTIIndicatorsConfig objects """

    model = OTIIndicatorsConfig
    serializer_class = OTIIndicatorsConfigSerializer


class OTIDemographicConfigViewSet(OTIAdminViewSet):
    """Viewset for OTIDemographicConfig objects """
    model = OTIDemographicConfig
    serializer_class = OTIDemographicConfigSerializer


class SamplePeriodViewSet(OTIAdminViewSet):
    """Viewset for SamplePeriod objects"""
    model = SamplePeriod
    lookup_field = 'type'
    serializer_class = SamplePeriodSerializer


def aggregation_filter(queryset, values):
    """Action/filter used to allow choosing multiple aggregation types

    The multiple choice filter field does not quite work and some pre-processing
    is necessary to allow comma-separated-values in query parameters
    """
    if not values:
        return queryset
    split_values = values.split(',')
    return queryset.filter(aggregation__in=split_values)


class IndicatorFilter(django_filters.FilterSet):
    """Custom filter for indicator"""

    sample_period = django_filters.CharFilter(name="sample_period__type")
    aggregation = django_filters.CharFilter(name="aggregation", action=aggregation_filter)
    city_name = django_filters.CharFilter(name="calculation_job__city_name")

    class Meta:
        model = Indicator
        fields = ['sample_period', 'type', 'aggregation', 'route_id',
                  'route_type', 'city_bounded', 'calculation_job', 'city_name']


"""Helper function to check that sample periods fall within the GTFS calendar date range.
    Returns true if all are valid.
"""
def valid_sample_periods():
    periods = SamplePeriod.objects.all()
    if len(periods) != 6:
        return False
    cursor = connection.cursor()
    cursor.execute('SELECT MIN(start_date), MAX(end_date) FROM gtfs_calendar;')
    start, end = cursor.fetchone()
    # model datetimes are stored as UTC; turn the date objects returned into UTC datetimes
    calendar_start = datetime.combine(start, time.min).replace(tzinfo=pytz.UTC)
    calendar_end = datetime.combine(end, time.max).replace(tzinfo=pytz.UTC)
    for period in periods:
        if period.type != 'alltime':
            if period.period_start < calendar_start or period.period_end > calendar_end:
                return False
    return True


class IndicatorJobViewSet(OTIAdminViewSet):
    """Viewset for IndicatorJobs"""
    model = IndicatorJob
    lookup_field = 'id'
    serializer_class = IndicatorJobSerializer
    filter_fields = ('job_status',)

    def create(self, request):
        """Override request to handle kicking off celery task"""

        indicators_config = OTIIndicatorsConfig.objects.all()[0]
        failures = []
        for attr in ['poverty_line', 'nearby_buffer_distance_m',
                     'max_commute_time_s', 'max_walk_time_s',
                     'avg_fare']:
            if not indicators_config.__getattribute__(attr) > 0:
                failures.append(attr)
        if not valid_sample_periods():
            failures.append('sample_periods')
        try:
            with connection.cursor() as c:
                c.execute('''SELECT COUNT(*) FROM planet_osm_line''')
        except ProgrammingError:
            failures.append('osm_data')

        if len(failures) > 0:
            response = Response({'error': 'Invalid configuration',
                                 'items': failures})
            response.status_code = status.HTTP_400_BAD_REQUEST
            return response

        response = super(IndicatorJobViewSet, self).create(request)
        if response.status_code == status.HTTP_201_CREATED:
            start_indicator_calculation.apply_async(args=[self.object.id], queue='indicators')
        return response


class LatestCalculationJob(APIView):
    def get(self, request, format=None):
        try:
            this_city = OTICityName.objects.all()[0].city_name
        except IndexError:
            this_city = 'My City'

        try:
            latest_job = IndicatorJob.objects.filter(job_status=IndicatorJob.StatusChoices.COMPLETE, city_name=this_city).order_by('-id')[0]
        except IndexError:
            return Response(None, status=status.HTTP_200_OK)

        serial_job = IndicatorJobSerializer(latest_job)
        return Response(serial_job.data, status=status.HTTP_200_OK)


class ScenarioViewSet(OTIBaseViewSet):
    """Viewset for Scenarios"""
    model = Scenario
    lookup_field = 'db_name'
    serializer_class = ScenarioSerializer
    filter_fields = ('job_status', 'created_by')

    def create(self, request):
        """Override request to handle kicking off celery task"""
        response = super(ScenarioViewSet, self).create(request)
        if response.status_code == status.HTTP_201_CREATED:
            start_scenario_creation.apply_async(args=[self.object.id], queue='scenarios')
        return response


class IndicatorViewSet(OTIAdminViewSet):
    """Viewset for Indicator objects

    Can be rendered as CSV in addition to the defaults
    Example CSV Export for all indicators calculated for the local GTFSFeed:
    GET /api/indicators/?format=csv&city_name=My%20City

    """
    model = Indicator
    serializer_class = IndicatorSerializer
    renderer_classes = api_settings.DEFAULT_RENDERER_CLASSES + [CSVRenderer]
    filter_class = IndicatorFilter
    filter_backends = api_settings.DEFAULT_FILTER_BACKENDS + [OrderingFilter]
    ordering = ('id', 'value')  # Default to standard id, but allow by value
    paginate_by = None
    paginate_by_param = 'page_size'
    max_paginate_by = 25

    def create(self, request, *args, **kwargs):
        """ Create Indicator objects via csv upload or json

        Upload csv with the following POST DATA:
        city_name: String field with city name to use as a reference in the
                   database for this indicator set
        source_file: File field with the csv file to import

        """
        # Handle as csv upload if form data present
        source_file = request.FILES.get('source_file', None)
        if source_file:
            city_name = request.DATA.pop('city_name', None)
            ## Moving city_name to IndicatorJob causes this to serialize to a list
            if city_name and type(city_name) is list:
                city_name = city_name.pop(0)
            load_status = Indicator.load(source_file, city_name, request.user)
            response_status = status.HTTP_200_OK if load_status.success else status.HTTP_400_BAD_REQUEST
            return Response(load_status.__dict__, status=response_status)

        # Fall through to JSON if no form data is present

        # If this is a post with many indicators, process as many
        is_many = isinstance(request.DATA, list)

        # Continue through normal serializer save process
        serializer = self.get_serializer(data=request.DATA, files=request.FILES, many=is_many)
        if serializer.is_valid():
            self.pre_save(serializer.object)
            self.object = serializer.save(force_insert=True)
            self.post_save(self.object, created=True)
            headers = self.get_success_headers(serializer.data)
            return Response(serializer.data, status=status.HTTP_201_CREATED,
                            headers=headers)

        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

    def delete(self, request, *args, **kwargs):
        """ Bulk delete of indicators based on a filter field

        Return 400 BAD REQUEST if no filter fields provided or all filter fields provided are
        not in delete_filter_fields
        e.g. We do not want to allow someone to delete all the indicators with a DELETE to this
        endpoint with no params

        """
        delete_filter_fields = {'city_name': 'calculation_job__city_name'}
        filters = []
        for field in request.QUERY_PARAMS:
            if field in delete_filter_fields:
                filters.append(field)

        if filters:
            indicators = Indicator.objects.all()
            for field in filters:
                indicators = indicators.filter(**{
                    delete_filter_fields.get(field): request.QUERY_PARAMS.get(field)})
            indicators.delete()
            return Response({}, status=status.HTTP_204_NO_CONTENT)

        return Response({'error': 'No valid filter fields'}, status=status.HTTP_400_BAD_REQUEST)


class IndicatorCalcJob(APIView):
    """ Indicator job id endpoint

    Returns currently valid indicator calculation jobs and associated city_name.
    Valid, here, is defined as:
        - IndicatorJob.id has at least one indicator in the Indicator table

    """
    def get(self, request, *args, **kwargs):
        """ Return the current calculations of indicators """
        try:
            current_ver = IndicatorJob.objects.latest('id').id
        except IndicatorJob.DoesNotExist:
            return Response({'current_jobs': []}, status=status.HTTP_200_OK)

        current_indicators = Indicator.objects.filter(calculation_job__exact=current_ver).values(
                'calculation_job', 'calculation_job__city_name').annotate()
        return Response({'current_jobs': current_indicators}, status=status.HTTP_200_OK)

        latest_job = IndicatorJob.objects.filter(job_status=IndicatorJob.StatusChoices.COMPLETE).order_by('-id')[0]


class IndicatorTypes(APIView):
    """ Indicator Types GET endpoint

    Returns a dict where key is the db indicator type key,
    display_name is the human readable, translated string description, and
    display_on_map is boolean indicating whether indicator should be shown on map or not

    """
    def get(self, request, *args, **kwargs):
        response = {}
        for key, value in Indicator.IndicatorTypes.CHOICES:
            if Indicator.IndicatorTypes.INDICATORS_TO_MAP.intersection([key]):
                show = True
            else:
                show = False
            response[key] = {'display_name': value, 'display_on_map': show}
        return Response(response, status=status.HTTP_200_OK)


class IndicatorAggregationTypes(APIView):
    """ Indicator Aggregation Types GET endpoint

    Returns a dict where key is the db indicator type key and value is
    the human readable, translated string description

    """
    def get(self, request, *args, **kwargs):
        response = {key: value for key, value in Indicator.AggregationTypes.CHOICES}
        return Response(response, status=status.HTTP_200_OK)




class IndicatorCities(APIView):
    """ Indicator Cities GET and DELETE endpoint

    Returns a list of city names that have indicators loaded
    Deletes Indicators and IndicatorJobs for a given city

    """
    def get(self, request, *args, **kwargs):
        response = IndicatorJob.objects.values_list('city_name', flat=True).filter(
                                                 city_name__isnull=False).distinct()
        return Response(response, status=status.HTTP_200_OK)

    def delete(self, request, *args, **kwargs):
        """ Bulk delete of indicators and indicator jobs based on a filter field

        Return 400 BAD REQUEST if no filter fields provided or all filter fields provided are
        not in delete_filter_fields
        e.g. We do not want to allow someone to delete all the indicators with a DELETE to this
        endpoint with no params

        """
        delete_filter_fields = {'city_name': 'calculation_job__city_name'}
        delete_indicatorjobs_filter_fields = {'city_name': 'city_name'}
        filters = []
        for field in request.QUERY_PARAMS:
            if field in delete_filter_fields:
                filters.append(field)

        if filters:
            indicators = Indicator.objects.all()
            indicatorjobs = IndicatorJob.objects.all()
            for field in filters:
                indicators = indicators.filter(**{
                    delete_filter_fields.get(field): request.QUERY_PARAMS.get(field)})
                indicatorjobs = indicatorjobs.filter(**{
                    delete_indicatorjobs_filter_fields.get(field): request.QUERY_PARAMS.get(field)})
            indicators.delete()
            indicatorjobs.delete()
            return Response({}, status=status.HTTP_204_NO_CONTENT)

        return Response({'error': 'No valid filter fields'}, status=status.HTTP_400_BAD_REQUEST)


class SamplePeriodTypes(APIView):
    """ Sample Period Types GET endpoint

    Returns a dict where key is the db indicator type key and value is
    the human readable, translated string description

    """
    def get(self, request, *args, **kwargs):
        response = {key: value for key, value in SamplePeriod.SamplePeriodTypes.CHOICES}
        return Response(response, status=status.HTTP_200_OK)


class GTFSRouteTypes(APIView):
    """ Endpoint to GET the gtfs route types from the database

    Pass GET param ?extended=1 to get the extended set of route_types, not just the gtfs core types

    """
    def get(self, request, *args, **kwargs):
        def is_used(route_type):
            routes = GTFSRoute.objects.all()
            return routes.filter(route_type=route_type).exists()

        get_extended = request.QUERY_PARAMS.get('extended', None)
        route_types = None
        if get_extended:
            route_types = GTFSRouteType.objects.all()
        else:
            # TODO: Move this filter logic to a static method on the GTFSRouteType class
            route_types = GTFSRouteType.objects.filter(route_type__lt=10)
        route_types = route_types.order_by('route_type')

        response = [{'route_type': rt.route_type, 'description': rt.get_route_type_display(),
                     'is_used': is_used(rt.route_type)} for rt in route_types]
        return Response(response, status=status.HTTP_200_OK)

latest_calculation = LatestCalculationJob.as_view()
indicator_calculation_job = IndicatorCalcJob.as_view()
indicator_types = IndicatorTypes.as_view()
indicator_jobs = IndicatorJobViewSet.as_view()
indicator_aggregation_types = IndicatorAggregationTypes.as_view()
indicator_cities = IndicatorCities.as_view()
sample_period_types = SamplePeriodTypes.as_view()
scenarios = ScenarioViewSet.as_view()
gtfs_route_types = GTFSRouteTypes.as_view()
city_name = OTICityNameView.as_view()
