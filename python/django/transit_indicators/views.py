from viewsets import OTIAdminViewSet
from models import OTIIndicatorsConfig, OTIDemographicConfig, PeakTravelPeriod
from serializers import (OTIIndicatorsConfigSerializer, OTIDemographicConfigSerializer,
                         PeakTravelPeriodSerializer)


class OTIIndicatorsConfigViewSet(OTIAdminViewSet):
    """ Viewset for OTIIndicatorsConfig objects """
    model = OTIIndicatorsConfig
    serializer_class = OTIIndicatorsConfigSerializer


class OTIDemographicConfigViewSet(OTIAdminViewSet):
    """Viewset for OTIDemographicConfig objects """
    model = OTIDemographicConfig
    serializer_class = OTIDemographicConfigSerializer


class PeakTravelPeriodViewSet(OTIAdminViewSet):
    """ Viewset for PeakTravelPeriod objects """
    model = PeakTravelPeriod
    serializer_class = PeakTravelPeriodSerializer
