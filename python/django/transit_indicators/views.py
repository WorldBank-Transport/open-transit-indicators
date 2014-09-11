import django_filters

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


class IndicatorFilter(django_filters.FilterSet):
    """Custom filter for indicator

    Allows searching for all the local instance indicators with GET param:
    local_city=True

    """
    local_city = django_filters.CharFilter(name="city_name", lookup_type="isnull")
    sample_period = django_filters.CharFilter(name="sample_period__type")

    class Meta:
        model = Indicator
        fields = ['sample_period', 'type', 'aggregation', 'route_id',
                  'route_type', 'city_bounded', 'version', 'city_name', 'local_city']


class IndicatorJobViewSet(OTIAdminViewSet):
    """Viewset for IndicatorJobs"""
    model = IndicatorJob
    lookup_field = 'version'
    serializer_class = IndicatorJobSerializer

    def create(self, request):
        """Override request to handle kicking off celery task"""
        response = super(IndicatorJobViewSet, self).create(request)
        if response.status_code == status.HTTP_201_CREATED:
            start_indicator_calculation.apply_async(args=[self.object.id], queue='indicators')
        return response


class IndicatorViewSet(OTIAdminViewSet):
    """Viewset for Indicator objects

    Can be rendered as CSV in addition to the defaults
    Example CSV Export for all indicators calculated for the local GTFSFeed:
    GET /api/indicators/?format=csv&local_city=True

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


class IndicatorVersion(APIView):
    """ Indicator versioning endpoint
    
    Returns most recent indicator version.

    """
    
    def get(self, request, *args, **kwargs):
        """ Return the current version of the indicators """
        try:
            job = IndicatorJob.objects.get(is_latest_version=True)
            version = job.version
            return Response({'current_version': version}, status=status.HTTP_200_OK)
        except IndicatorJob.DoesNotExist:
            return Response({'current_version': None}, status=status.HTTP_200_OK)
        except IndicatorJob.MultipleObjectsReturned:
            return Response({'error':'Multiple versions found'},
                             status=status.HTTP_500_INTERNAL_SERVER_ERROR)

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
