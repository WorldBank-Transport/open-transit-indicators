from django.db import models
from django.db.models.signals import post_save
from django.dispatch import receiver


class DataSource(models.Model):
    """Base model for data source models.

    E.g. GTFS feeds, OSM data, Shapefiles with population statistics."""

    create_date = models.DateTimeField(auto_now_add=True)
    last_modify_date = models.DateTimeField(auto_now=True)

    # May want to move this into a mixin eventually.
    source_file = models.FileField()

    class Meta(object):
        abstract = True


class GTFSFeed(DataSource):
    """Represents a GTFS Feed (a zip file)."""
    is_valid = models.NullBooleanField()
    is_processed = models.BooleanField(default=False)


class GTFSFeedProblem(models.Model):
    """Problem (either a warning or error) for a GTSFeed object"""

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
    gtfsfeed = models.ForeignKey(GTFSFeed)
