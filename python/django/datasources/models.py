from django.db import models


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
