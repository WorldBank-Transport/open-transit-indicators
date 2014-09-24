from django.conf import settings
from django.contrib.gis.db import models
from django.utils.translation import ugettext_lazy as _


class DataSource(models.Model):
    """Base model for data source models.

    E.g. GTFS feeds, OSM data, Shapefiles with population statistics."""

    create_date = models.DateTimeField(auto_now_add=True)
    last_modify_date = models.DateTimeField(auto_now=True)
    # make user-configurable with upload
    city_name = models.CharField(max_length=255, default=settings.OTI_CITY_NAME)

    class Meta(object):
        abstract = True


class FileDataSource(DataSource):
    """DataSource with a source_file field and processing statuses."""

    class Statuses(object):
        PENDING = 'pending'
        UPLOADING = 'uploading'
        UPLOADED = 'uploaded'
        PROCESSING = 'processing'
        DOWNLOADING = 'downloading'
        WAITING_USER_INPUT = 'waiting_input'
        VALIDATING = 'validating'
        IMPORTING = 'importing'
        COMPLETE = 'complete'
        WARNING = 'warning'
        ERROR = 'error'
        CHOICES = (
            (PENDING, _(u'Pending')),
            (UPLOADING, _(u'Uploading')),
            (UPLOADED, _(u'Uploaded')),
            (PROCESSING, _(u'Processing')),
            (DOWNLOADING, _(u'Downloading')),
            (WAITING_USER_INPUT, _(u'Waiting for User Input')),
            (VALIDATING, _(u'Validating')),
            (COMPLETE, _(u'Complete')),
            (WARNING, _(u'Warning')),
            (ERROR, _(u'Error')),
        )

    source_file = models.FileField()
    status = models.CharField(max_length=16, default=Statuses.PENDING,
                              choices=Statuses.CHOICES)

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
    """Represents a GTFS Feed (a zip file).

    Uses the statuses:
        PENDING: Waiting for job to be taken
        VALIDATING: Validating gtfs feed zip file via the python gtfs validator
        PROCESSING: Loading the gtfs data (if valid) into the local database
        COMPLETE: GTFS data successfully validated and loaded into the local database
        ERROR: An error occurred during processing. Check related instances
               of the GTFSFeedProblem endpoint
    """


class GTFSFeedProblem(DataSourceProblem):
    """Problem (either a warning or error) for a GTSFeed object"""
    gtfsfeed = models.ForeignKey(GTFSFeed)


class Boundary(FileDataSource):
    """A boundary, used for denoting cities and regions. Created from a Shapefile.

    Uses the statuses:
        PENDING: Waiting for job to be taken
        IMPORTING: Processing and loading the shapefile into the database
        COMPLETE: Boundary shapefile successfully loaded
        ERROR: Error during processing. Check related instances of BoundaryProblem

    """
    # Since we can't determine the SRID at runtime, Django will store
    # everything in WebMercator; indicator calculations will need to
    # use a transformed version or their calculations will be inaccurate.
    geom = models.MultiPolygonField(srid=settings.DJANGO_SRID, blank=True, null=True)


class OSMData(FileDataSource):
    """Represents OSM Import

    Uses the statuses:
        PENDING: Waiting for the job to be taken
        PROCESSING: Calculating the bbox to download data for
        DOWNLOADING: Downloading applicable open street map data
        IMPORTING: Importing downloaded data into the local db
        COMPLETE: Import done
        ERROR: An error occurred during processing. Check related OSMDataProblem endpoint
    """
    gtfsfeed = models.ForeignKey(GTFSFeed)


class OSMDataProblem(DataSourceProblem):
    """Problem (warning or error) with an OSM import"""
    osmdata = models.ForeignKey(OSMData)


class RealTime(FileDataSource):
    """ Represents a stop_times.txt_new upload

    File must be generated via Mike Smith's Stop Times software
    Loads data to gtfs_realtime.RealStopTime via celery

    Uses the statuses:
        PENDING: Waiting for job to be taken
        IMPORTING: Importing the stop times into the local database
        COMPLETE: Stop times successfully loaded
        ERROR: Error during processing. Check related instances of RealTimeProblem

    """


class RealTimeProblem(DataSourceProblem):
    """ Problem with a RealTime import """
    realtime = models.ForeignKey(RealTime)


class BoundaryProblem(DataSourceProblem):
    """Problem (warning or error) with a Boundary."""
    boundary = models.ForeignKey(Boundary)


class DemographicDataSource(FileDataSource):
    """Stores a shapefile containing demographic data; actual data is in DemographicDataFeature.

    Uses the statuses:
        PENDING: Waiting for a job to be taken, either the initial user selections or
                 the data load after the user selects appropriate fields
        PROCESSING: Extracting the valid fields from the uploaded demographic shapefile
        WAITING_USER_INPUT: Fields extracted, waiting for user to select fields
                            The DemographicDataSource object also reverts to this status
                            if there was an error processing the user's field selections.
                            Check the related DemographicDataSourceProblem objects
        IMPORTING: Importing data from the user-selected demographic fields in the uploaded
                   shapefile
        COMPLETE: Demographic data from the shapefile loaded into local database
        ERROR: Error during PROCESING step

    """
    # Number of features detected in the shapefile associated with this model.
    # Lets us know if we were able to create all the DemographicDataFeatures
    # correctly.
    num_features = models.PositiveIntegerField(blank=True, null=True)


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
