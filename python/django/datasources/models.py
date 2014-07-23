from django.conf import settings
from django.contrib.gis.db import models


class DataSource(models.Model):
    """Base model for data source models.

    E.g. GTFS feeds, OSM data, Shapefiles with population statistics."""

    create_date = models.DateTimeField(auto_now_add=True)
    last_modify_date = models.DateTimeField(auto_now=True)

    class Meta(object):
        abstract = True


class FileDataSource(DataSource):
    """DataSource with a source_file field and processing statuses."""
    source_file = models.FileField()
    is_valid = models.NullBooleanField()
    is_processed = models.BooleanField(default=False)

    class Meta(object):
        abstract = True


class DataSourceProblem(models.Model):
    """ Base model for problems (warnings or errors) with data sources.

    Implement by subclassing and adding a ForeignKey to the DataSource class you need.
    """
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


class GTFSFeed(FileDataSource):
    """Represents a GTFS Feed (a zip file)."""


class GTFSFeedProblem(DataSourceProblem):
    """Problem (either a warning or error) for a GTSFeed object"""
    gtfsfeed = models.ForeignKey(GTFSFeed)


class Boundary(FileDataSource):
    """A boundary, used for denoting cities and regions. Created from a Shapefile."""
    # Since we can't determine the SRID at runtime, Django will store
    # everything in WebMercator; indicator calculations will need to
    # use a transformed version or their calculations will be inaccurate.
    geom = models.MultiPolygonField(srid=settings.DJANGO_SRID, blank=True, null=True)


class BoundaryProblem(DataSourceProblem):
    """Problem (warning or error) with a Boundary."""
    boundary = models.ForeignKey(Boundary)
