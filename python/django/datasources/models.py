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
    validation_results_file = models.FileField(null=True, blank=True)
