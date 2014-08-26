import django_filters

from rest_framework import status
from rest_framework.decorators import action
from rest_framework.parsers import MultiPartParser
from rest_framework.response import Response
from rest_framework.settings import api_settings
from rest_framework.views import APIView
from rest_framework_csv.renderers import CSVRenderer

from viewsets import OTIAdminViewSet
from models import OTIIndicatorsConfig, OTIDemographicConfig, SamplePeriod, Indicator
from serializers import (OTIIndicatorsConfigSerializer, OTIDemographicConfigSerializer,
                         SamplePeriodSerializer, IndicatorSerializer)


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

    class Meta:
        model = Indicator
        fields = ['sample_period', 'type', 'aggregation', 'route_id',
                  'route_type', 'city_bounded', 'version', 'city_name', 'local_city']

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
            load_status = Indicator.load(source_file, city_name)
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


class IndicatorsVersion(APIView):
    """ Indicator versioning endpoint

    Currently just gets the highest integer since versions are timestamps
    Could add a POST method here to set the version if versioning gets more complicated
    TODO: Update logic once we actually solve versioning

    """
    def get(self, request, *args, **kwargs):
        """ Return the current version of the indicators """
        indicator = Indicator.objects.order_by('-version')[0]
        version = indicator.version if indicator and indicator.version else None
        return Response({'current_version': version }, status=status.HTTP_200_OK)


indicators_version = IndicatorsVersion.as_view()
