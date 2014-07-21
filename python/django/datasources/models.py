from django.conf import settings
from django.contrib.gis.db import models


class DataSource(models.Model):
    """Base model for data source models.

    E.g. GTFS feeds, OSM data, Shapefiles with population statistics."""

    create_date = models.DateTimeField(auto_now_add=True)
    last_modify_date = models.DateTimeField(auto_now=True)

    # May want to move this into a mixin eventually.
    source_file = models.FileField()

    class Meta(object):
        abstract = True


class DataSourceProblem(models.Model):
    """ Base model for problems (warnings or errors) with data sources.

    Implement by subclassing and adding a ForeignKey to the DataSource class you need.
    If you want to take advantage of the DataSourceProblemCountMixin in serializers.py, name
    your DataSourceProblem subclass '{{Name of DataSource subclass}}Problem'."""
    class ProblemTypes(object):
        ERROR = 'err'
        WARNING = 'war'
        CHOICES = (
            (ERROR, 'Error'),
            (WARNING, 'Warning'),
        )
    type = models.CharField(max_length=3,
                            choices=ProblemTypes.CHOICES)
    description = models.TextField()
    title = models.CharField(max_length=255)

    class Meta(object):
        abstract = True


class GTFSFeed(DataSource):
    """Represents a GTFS Feed (a zip file)."""
    is_valid = models.NullBooleanField()
    is_processed = models.BooleanField(default=False)


class GTFSFeedProblem(DataSourceProblem):
    """Problem (either a warning or error) for a GTSFeed object"""
    gtfsfeed = models.ForeignKey(GTFSFeed)


class Boundary(DataSource):
    """A boundary, used for denoting cities and regions. Created from a Shapefile."""
    is_valid = models.NullBooleanField()
    is_processed = models.BooleanField(default=False)

    # Since we can't determine the SRID at runtime, Django will store
    # everything in WebMercator; indicator calculations will need to
    # use a transformed version or their calculations will be inaccurate.
    geom = models.MultiPolygonField(srid=settings.DJANGO_SRID, blank=True, null=True)


class BoundaryProblem(DataSourceProblem):
    """Problem (warning or error) with a Boundary."""
    boundary = models.ForeignKey(Boundary)
