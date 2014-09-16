import django_filters

from django.conf import settings

from rest_framework import status
from rest_framework.decorators import action
from rest_framework.response import Response
from rest_framework.settings import api_settings
from rest_framework.views import APIView
from rest_framework_csv.renderers import CSVRenderer

from viewsets import OTIAdminViewSet
from models import (OTIIndicatorsConfig,
                    OTIDemographicConfig,
                    SamplePeriod,
                    Indicator,
                    IndicatorJob,
                    GTFSRouteType)
from transit_indicators.tasks import start_indicator_calculation
from serializers import (OTIIndicatorsConfigSerializer, OTIDemographicConfigSerializer,
                         SamplePeriodSerializer, IndicatorSerializer, IndicatorJobSerializer)


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
    """Custom filter for indicator

    For is_latest_version must pass pythons boolean, True|False as the value:
    e.g. indicators?is_latest_version=True

    TODO: Filter all but the most recent version for each city in the sent response

    """
    sample_period = django_filters.CharFilter(name="sample_period__type")
    aggregation = django_filters.CharFilter(name="aggregation", action=aggregation_filter)
    is_latest_version = django_filters.BooleanFilter(name="version__is_latest_version")

    class Meta:
        model = Indicator
        fields = ['sample_period', 'type', 'aggregation', 'route_id',
                  'route_type', 'city_bounded', 'version', 'city_name', 'is_latest_version']


class IndicatorJobViewSet(OTIAdminViewSet):
    """Viewset for IndicatorJobs"""
    model = IndicatorJob
    lookup_field = 'version'
    serializer_class = IndicatorJobSerializer

    def create(self, request):
        """Override request to handle kicking off celery task"""
        response = super(IndicatorJobViewSet, self).create(request)
        if response.status_code == status.HTTP_201_CREATED:
            start_indicator_calculation.apply_async(args=[self.object.id, settings.OTI_CITY_NAME], queue='indicators')
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
            city_name = request.DATA.get('city_name', None)
            load_status = Indicator.load(source_file, city_name, request.user)
            response_status = status.HTTP_200_OK if load_status.success else status.HTTP_400_BAD_REQUEST
            return Response(load_status.__dict__, status=response_status)

        # Fall through to JSON if no form data is present

        # If this is a post with many indicators, process as many
        if isinstance(request.DATA, list):
            many=True
        else:
            many=False

        # Continue through normal serializer save process
        serializer = self.get_serializer(data=request.DATA, files=request.FILES, many=many)
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
        delete_filter_fields = ('city_name')
        filters = []
        for field in request.QUERY_PARAMS:
            if field in delete_filter_fields:
                filters.append(field)

        if filters:
            indicators = Indicator.objects.all();
            for field in filters:
                indicators = indicators.filter(**{field: request.QUERY_PARAMS[field]})
            indicators.delete()
            return Response({}, status=status.HTTP_204_NO_CONTENT)

        return Response({'error': 'No valid filter fields'}, status=status.HTTP_400_BAD_REQUEST)


class IndicatorVersion(APIView):
    """ Indicator versioning endpoint

    Returns currently valid indicator versions and associated city_name. Valid defined as:
        - IndicatorJob.version has at least one indicator in the Indicator table
        - IndicatorJob.is_latest_version = True

    """
    def get(self, request, *args, **kwargs):
        """ Return the current versions of the indicators """
        versions = Indicator.objects.filter(version__is_latest_version=True).values('version', 'city_name').annotate()
        return Response({'current_versions': versions}, status=status.HTTP_200_OK)


class IndicatorTypes(APIView):
    """ Indicator Types GET endpoint

    Returns a dict where key is the db indicator type key and value is
    the human readable, translated string description

    """
    def get(self, request, *args, **kwargs):
        response = { key: value for key, value in Indicator.IndicatorTypes.CHOICES }
        return Response(response, status=status.HTTP_200_OK)


class IndicatorAggregationTypes(APIView):
    """ Indicator Aggregation Types GET endpoint

    Returns a dict where key is the db indicator type key and value is
    the human readable, translated string description

    """
    def get(self, request, *args, **kwargs):
        response = { key: value for key, value in Indicator.AggregationTypes.CHOICES }
        return Response(response, status=status.HTTP_200_OK)


class IndicatorCities(APIView):
    """ Indicator Cities GET endpoint

    Returns a list of city names that have indicators loaded

    """
    def get(self, request, *args, **kwargs):
        response = Indicator.objects.values_list('city_name', flat=True).filter(
                                                 city_name__isnull=False).distinct()
        return Response(response, status=status.HTTP_200_OK)


class SamplePeriodTypes(APIView):
    """ Sample Period Types GET endpoint

    Returns a dict where key is the db indicator type key and value is
    the human readable, translated string description

    """
    def get(self, request, *args, **kwargs):
        response = { key: value for key, value in SamplePeriod.SamplePeriodTypes.CHOICES }
        return Response(response, status=status.HTTP_200_OK)


class GTFSRouteTypes(APIView):
    """ Endpoint to GET the gtfs route types from the database

    Pass GET param ?extended=1 to get the extended set of route_types, not just the gtfs core types

    """
    def get(self, request, *args, **kwargs):
        get_extended = request.QUERY_PARAMS.get('extended', None)
        route_types = None
        if get_extended:
            route_types = GTFSRouteType.objects.all()
        else:
            # TODO: Move this filter logic to a static method on the GTFSRouteType class
            route_types = GTFSRouteType.objects.filter(route_type__lt=10)
        route_types = route_types.order_by('route_type')

        response = [{ 'route_type': rt.route_type, 'description': rt.description } for rt in route_types]
        return Response(response, status=status.HTTP_200_OK)


indicator_version = IndicatorVersion.as_view()
indicator_types = IndicatorTypes.as_view()
indicator_jobs = IndicatorJobViewSet.as_view()
indicator_aggregation_types = IndicatorAggregationTypes.as_view()
indicator_cities = IndicatorCities.as_view()
sample_period_types = SamplePeriodTypes.as_view()
gtfs_route_types = GTFSRouteTypes.as_view()
