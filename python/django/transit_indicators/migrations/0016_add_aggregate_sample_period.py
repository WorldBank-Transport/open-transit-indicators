# -*- coding: utf-8 -*-
import datetime
from django.utils.timezone import utc
from django.db import migrations

def add_aggregate_sample_period(apps, schema_editor):
    """Adds a sample period row that represents an aggregate"""
    sample_period = apps.get_model('transit_indicators', 'SamplePeriod')

    # This is what datetime.min and datetime.max are set to behind the scenes,
    # with the addition of a tzinfo, which the app expects.
    # These values won't actually be used for anything.
    start = datetime.datetime(datetime.MINYEAR, 1, 1, tzinfo=utc)
    end = datetime.datetime(datetime.MAXYEAR, 12, 31, 23, 59, 59, 999999, tzinfo=utc)
    sample_period.objects.create(type='alltime', period_start=start, period_end=end)

class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0015_indicator_the_geom'),
    ]

    operations = [
        migrations.RunPython(add_aggregate_sample_period),
    ]
