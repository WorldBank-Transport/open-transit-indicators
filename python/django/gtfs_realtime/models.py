from django.db import models

from datasources.models import RealTime


class AbstractStopTime(models.Model):

    trip_id = models.CharField(max_length=255)

    stop_id = models.CharField(max_length=255)

    stop_sequence = models.IntegerField()

    ## Char fields to match scala's definition of gtfs_stop_types
    arrival_time = models.CharField(max_length=12)
    departure_time = models.CharField(max_length=12)

    stop_headsign = models.CharField(max_length=255, null=True, blank=True)

    pickup_type = models.IntegerField(default=0)

    drop_off_type = models.IntegerField(default=0)

    shape_dist_traveled = models.FloatField(null=True, blank=True)

    class Meta(object):
        abstract = True


class RealStopTime(AbstractStopTime):

    datasource = models.ForeignKey(RealTime)

    class Meta(AbstractStopTime.Meta):
        db_table = 'gtfs_stop_times_real'
