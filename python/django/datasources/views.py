"""Endpoints for data sources."""

from rest_framework import viewsets

from models import GTFSFeed
from serializers import GTFSFeedSerializer
from transit_indicators.tasks import verify_gtfs


class GTFSFeedViewSet(viewsets.ModelViewSet):
    """View set for dealing with GTFS Feeds."""
    model = GTFSFeed
    serializer_class = GTFSFeedSerializer

    def create(self, request):
        response = super(GTFSFeedViewSet, self).create(request)
        self.go_validate_feed(response)
        return response

    def update(self, request, pk=None):
        response = super(GTFSFeedViewSet, self).update(request, pk)
        self.go_validate_feed(response)
        return response

    def go_validate_feed(self, response):
        """Get GTFS ID from POST response and tell celery to run validation task for the feed.

        Argument: response to POST from the REST API
        """
        feed_id = response.data.get('id')
        print('feed id: ' + str(feed_id))
        if feed_id:
            # go kick off celery task to asynchronously validate the GTFS
            verify_gtfs.delay(feed_id)
