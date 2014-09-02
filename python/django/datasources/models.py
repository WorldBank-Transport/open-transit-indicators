from django.conf import settings
from django.contrib.gis.db import models
from django.utils.translation import ugettext_lazy as _


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
            (ERROR, _(u'Error')),
            (WARNING, _(u'Warning')),
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


class OSMData(DataSource):
    """Represents OSM Import"""
    source_file = models.FileField(null=True, blank=True)
    gtfsfeed = models.ForeignKey(GTFSFeed)
    is_downloaded = models.BooleanField(default=False)
    source_file = models.FileField()
    is_valid = models.NullBooleanField()
    is_processed = models.BooleanField(default=False)


class OSMDataProblem(DataSourceProblem):
    """Problem (warning or error) with an OSM import"""
    osmdata = models.ForeignKey(OSMData)


class BoundaryProblem(DataSourceProblem):
    """Problem (warning or error) with a Boundary."""
    boundary = models.ForeignKey(Boundary)


class DemographicDataSource(FileDataSource):
    """Stores a shapefile containing demographic data; actual data is in DemographicDataFeature."""
    # Number of features detected in the shapefile associated with this model.
    # Lets us know if we were able to create all the DemographicDataFeatures
    # correctly.
    num_features = models.PositiveIntegerField(blank=True, null=True)

    # Whether the task to finish loading the datasource into
    # DemographicDataFeatures has finished.
    is_loaded = models.BooleanField(default=False)


class DemographicDataSourceProblem(DataSourceProblem):
    """Problem with a demographic data shapefile."""
    datasource = models.ForeignKey(DemographicDataSource)


class DemographicDataFieldName(models.Model):
    """Name of a field in a DemographicAvailableData's Shapefile."""
    # Shapefiles limit column names to 10 characters
    name = models.CharField(max_length=10, db_index=True)
    datasource = models.ForeignKey(DemographicDataSource)

    def __repr__(self):
        return self.name

    def __str__(self):
        return str(self.__repr__())

    def __unicode__(self):
        return unicode(self.__repr__())


class DemographicDataFeature(DataSource):
    """Stores demographic data associated with a geometry.

    Users are explicitly limited to having two population metrics and one destination metric
    (e.g. available jobs). This simplifies data storage and processing.
    """

    # Metrics about the population, e.g. number of people aged > 65
    population_metric_1 = models.FloatField(blank=True, null=True)
    population_metric_2 = models.FloatField(blank=True, null=True)

    # Metrics about destinations in a given geometry, e.g. number of jobs.
    destination_metric_1 = models.FloatField(blank=True, null=True)

    # The geometry to which the metrics apply.
    geom = models.MultiPolygonField(srid=settings.DJANGO_SRID)

    # The DataSource where this data came from
    datasource = models.ForeignKey(DemographicDataSource)
