"""Endpoints for data sources."""

from rest_framework import viewsets

from models import GTFSFeed


class GTFSFeedViewSet(viewsets.ModelViewSet):
    """View set for dealing with GTFS Feeds."""
    model = GTFSFeed
