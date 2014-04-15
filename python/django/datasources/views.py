"""Endpoints for data sources."""

from rest_framework import viewsets, status

from datasources.models import GTFSFeed, GTFSFeedProblem
from datasources.serializers import GTFSFeedSerializer
from datasources.tasks import validate_gtfs

class GTFSFeedViewSet(viewsets.ModelViewSet):
    """View set for dealing with GTFS Feeds."""
    model = GTFSFeed
    serializer_class = GTFSFeedSerializer

    def create(self, request):
        """Override create method to call validation task with celery"""
        response = super(GTFSFeedViewSet, self).create(request)
        if response.status_code == status.HTTP_201_CREATED:
            validate_gtfs.delay(self.object.id)
        return response

    def update(self, request, pk=None):
        """Override update to re-validate GTFS"""
        response = super(GTFSFeedViewSet, self).update(request, pk)

        # Reset processing status since GTFS needs revalidation
        self.object.is_processed = False
        self.object.save()
        response.data['is_processed'] = False

        # Delete existing problems since it will be revalidated
        self.obj.gtfsfeedproblem_set.all().delete()

        validate_gtfs.delay(self.object.id)
        return response

class GTFSFeedProblemViewSet(viewsets.ModelViewSet):
    """Viewset for displaying problems for GTFS data"""
    model = GTFSFeedProblem
    filter_fields = ('gtfsfeed',)