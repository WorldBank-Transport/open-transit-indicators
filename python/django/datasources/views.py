"""Endpoints for data sources."""

import sys
from django.db.models import Max, Min
from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from rest_framework.views import APIView

from transit_indicators.viewsets import OTIAdminViewSet
from datasources.viewsets import FileDataSourceViewSet
from datasources.models import (GTFSFeed, GTFSFeedProblem, Boundary, BoundaryProblem,
                                RealTime, RealTimeProblem, FileDataSource,
                                DemographicDataSource, DemographicDataSourceProblem,
                                DemographicDataFeature, OSMData, OSMDataProblem)
from datasources.serializers import (GTFSFeedSerializer, BoundarySerializer, RealTimeSerializer,
                                     DemographicDataSourceSerializer, OSMDataSerializer)
from datasources.tasks import (validate_gtfs, shapefile_to_boundary, get_shapefile_fields,
                               load_shapefile_data, import_osm_data, import_real_time_data)
from transit_indicators.models import OTIDemographicConfig
from transit_indicators.serializers import OTIDemographicConfigSerializer


class GTFSFeedViewSet(FileDataSourceViewSet):
    """View set for dealing with GTFS Feeds."""
    model = GTFSFeed
    serializer_class = GTFSFeedSerializer

    def create(self, request):
        """Override create method to call validation task with celery"""
        response = super(GTFSFeedViewSet, self).create(request)
        if response.status_code == status.HTTP_201_CREATED:
            validate_gtfs.apply_async(args=[self.object.id], queue='datasources')
        return response


class GTFSFeedProblemViewSet(OTIAdminViewSet):
    """Viewset for displaying problems for GTFS data"""
    model = GTFSFeedProblem
    filter_fields = ('gtfsfeed',)


class RealTimeViewSet(FileDataSourceViewSet):
    """ View set for dealing wth RealTime uploads """
    model = RealTime
    serializer_class = RealTimeSerializer

    def create(self, request):
        """ Override create to load realtime data via geotrellis """
        response = super(RealTimeViewSet, self).create(request)
        if response.status_code == status.HTTP_201_CREATED:
            import_real_time_data.apply_async(args=[self.object.id], queue='datasources')
        return response


class RealTimeProblemViewSet(OTIAdminViewSet):
    """ View set for dealing with RealTime problems """
    model = RealTimeProblem
    filter_fields = ('realtime',)


class OSMDataViewSet(FileDataSourceViewSet):
    """View set for dealing with OSM Feeds."""
    model = OSMData
    serializer_class = OSMDataSerializer

    def create(self, request):
        """Override create method to call import task with celery"""
        response = super(OSMDataViewSet, self).create(request)
        if response.status_code == status.HTTP_201_CREATED:
            import_osm_data.apply_async(args=[self.object.id], queue='datasources')
        return response

    def update(self, request, pk=None):
        """Override update to re-import OSMData"""
        response = super(OSMDataViewSet, self).update(request, pk)

        # Reset processing status since osm needs reimportation
        status = OSMData.Statuses.PENDING
        self.object.status = status
        self.object.save()
        response.data['status'] = status

        # Delete existing problems since it will be revalidated
        self.obj.osmdataproblem_set.all().delete()

        import_osm_data.apply_async(args=[self.object.id], queue='datasources')
        return response


class OSMDataProblemsViewSet(OTIAdminViewSet):
    """Viewset for displaying problems for OSM data"""
    model = OSMDataProblem
    filter_fields = ('osmdata',)


class BoundaryViewSet(FileDataSourceViewSet):
    """View set for handling boundaries (city, regional)."""
    model = Boundary
    serializer_class = BoundarySerializer

    def create(self, request):
        """Run validation / import task via celery on creation."""
        response = super(BoundaryViewSet, self).create(request)
        if response.status_code == status.HTTP_201_CREATED:
            shapefile_to_boundary.apply_async(args=[self.object.id], queue='datasources')
        return response


class BoundaryProblemViewSet(OTIAdminViewSet):
    """Viewset for displaying BoundaryProblems (generated while processing shapefiles)."""
    model = BoundaryProblem
    filter_fields = ('boundary',)


class DemographicDataSourceViewSet(FileDataSourceViewSet):
    """Display and create sets of demographic data by uploading shapefiles.
    A POST to this view with a Shapefile will kick off a Celery job to grab the
    data fields from the Shapefile, and validates the Shapefile in the process.

    Once the associated DataSource has status == WAITING_USER_INPUT
    the DataSource's 'fields' field will contain a list of strings representing the
    data fields available in the shapefile.

    At this point, a POST to /<this-view>/<datasource-id>/load/ with a JSON string
    representing a valid transit_indicators.OTIDemographicConfig object will cause
    the specified fields from the shapefile to be loaded into the database and the
    app configuration to be updated.

    Subsequent POSTs will delete and replace the loaded data."""
    model = DemographicDataSource
    serializer_class = DemographicDataSourceSerializer

    def create(self, request):
        """Validate shapefile and extract column headings."""
        response = super(DemographicDataSourceViewSet, self).create(request)
        if response.status_code == status.HTTP_201_CREATED:
            get_shapefile_fields.apply_async(args=[self.object.id], queue='datasources')
        return response

    @action()
    def load(self, request, pk=None):
        """Load the DataSource into DemographicDataFeatures."""
        demog_data = self.get_object()
        # Updating existing configuration, if it exists
        config_obj = None
        if OTIDemographicConfig.objects.all().count() > 0:
            config_obj = OTIDemographicConfig.objects.all()[0]

        serializer = OTIDemographicConfigSerializer(data=dict(datasource=demog_data.id,
                                                              **request.DATA),
                                                    instance=config_obj)
        if not serializer.is_valid():
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
        else:
            serializer.object.save()
            demog_data.status = DemographicDataSource.Statuses.PENDING
            demog_data.save()
            load_shapefile_data.apply_async(args=[demog_data.id,
                                                  serializer.data.get('pop_metric_1_field', None),
                                                  serializer.data.get('pop_metric_2_field', None),
                                                  serializer.data.get('dest_metric_1_field', None)],
                                            queue='datasources')
            return Response({'status': 'Started loading data.'})


class DemographicDataSourceProblemViewSet(OTIAdminViewSet):
    """Viewset for displaying BoundaryProblems (generated while processing shapefiles)."""
    model = DemographicDataSourceProblem
    filter_fields = ('datasource',)


class DemographicDataFeatureViewSet(OTIAdminViewSet):
    """Demographic data associated with geographic features."""
    model = DemographicDataFeature
    filter_fields = ('datasource',)


class DemographicDataRanges(APIView):
    """ Endpoint to GET the min/max ranges of demographic data

    GET params:
      type: String (required), Name of the demographic data field to use for range calculation
    """
    def get(self, request, *args, **kwargs):
        get_type = request.QUERY_PARAMS.get('type', None)
        if not get_type:
            return Response("type parameter is required", status=status.HTTP_200_OK)

        agg = DemographicDataFeature.objects.all().aggregate(min=Min(get_type), max=Max(get_type))
        return Response(agg, status=status.HTTP_200_OK)


class UploadStatusChoices(APIView):
    """ Return an object of the available upload statuses

    Each entry is a status, where the key is the database key
    and the value is the human-readable, translated string

    """
    def get(self, request, *args, **kwargs):
        response = { status[0]: status[1] for status in FileDataSource.Statuses.CHOICES }
        return Response(response, status=status.HTTP_200_OK)


demographic_data_ranges = DemographicDataRanges.as_view()
upload_status_choices = UploadStatusChoices.as_view()
