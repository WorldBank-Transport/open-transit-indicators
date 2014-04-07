"""Endpoints for data sources."""

from rest_framework import viewsets

from models import GTFSFeed
from serializers import GTFSFeedSerializer


class GTFSFeedViewSet(viewsets.ModelViewSet):
    """View set for dealing with GTFS Feeds."""
    model = GTFSFeed
    serializer_class = GTFSFeedSerializer
