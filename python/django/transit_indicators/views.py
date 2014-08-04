from viewsets import OTIAdminViewSet
from models import OTIIndicatorsConfig, OTIDemographicConfig, SamplePeriod
from serializers import (OTIIndicatorsConfigSerializer, OTIDemographicConfigSerializer,
                         SamplePeriodSerializer)


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
    serializer_class = SamplePeriodSerializer
