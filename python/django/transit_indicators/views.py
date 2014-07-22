from rest_framework import viewsets

from viewsets import OTIAdminViewSet
from models import OTIIndicatorsConfig, PeakTravelPeriod
from serializers import OTIIndicatorsConfigSerializer, PeakTravelPeriodSerializer


class OTIIndicatorsConfigViewSet(OTIAdminViewSet):
    """ Viewset for OTIIndicatorsConfig objects """
    model = OTIIndicatorsConfig
    serializer_class = OTIIndicatorsConfigSerializer


class PeakTravelPeriodViewSet(OTIAdminViewSet):
    """ Viewset for PeakTravelPeriod objects """
    model = PeakTravelPeriod
    serializer_class = PeakTravelPeriodSerializer
