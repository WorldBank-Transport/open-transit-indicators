from rest_framework import viewsets
from rest_framework.permissions import IsAuthenticated

from transit_indicators.permissions import IsAuthenticatedAndAdminUserOrReadOnly, IsAdminOrScenario


class OTIBaseViewSet(viewsets.ModelViewSet):
    """Viewset permissions for OTI Model objects"""
    # Django Model Permissions includes IsAuthenticated
    permission_classes = [IsAuthenticated]


class OTIIndicatorViewSet(viewsets.ModelViewSet):
	"""Viewset permissions for indicator jobs"""
	permission_classes = [IsAdminOrScenario]


class OTIAdminViewSet(viewsets.ModelViewSet):
    """Viewset permissions for the OTI Admin/Settings objects

    e.g. OTIConfiguration model

    """
    permission_classes = [IsAuthenticatedAndAdminUserOrReadOnly]
