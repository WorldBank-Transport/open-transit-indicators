from rest_framework import viewsets

from models import OTIIndicatorsConfig, PeakTravelPeriod
from serializers import OTIIndicatorsConfigSerializer, PeakTravelPeriodSerializer


class OTIIndicatorsConfigViewSet(viewsets.ModelViewSet):
    """ Viewset for OTIIndicatorsConfig objects """
    model = OTIIndicatorsConfig
    serializer_class = OTIIndicatorsConfigSerializer


class PeakTravelPeriodViewSet(viewsets.ModelViewSet):
    """ Viewset for PeakTravelPeriod objects """
    model = PeakTravelPeriod
    serializer_class = PeakTravelPeriodSerializer
