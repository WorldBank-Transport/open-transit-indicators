from rest_framework.settings import api_settings
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


class IndicatorViewSet(OTIAdminViewSet):
    """Viewset for Indicator objects

    Can be rendered as CSV in addition to the defaults

    """
    model = Indicator
    serializer_class = IndicatorSerializer
    renderer_classes = api_settings.DEFAULT_RENDERER_CLASSES + [CSVRenderer]
    filter_fields = ('sample_period', 'type', 'aggregation', 'route_id',
                     'route_type', 'city_bounded', 'version',)
