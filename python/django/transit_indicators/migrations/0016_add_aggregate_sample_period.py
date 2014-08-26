# -*- coding: utf-8 -*-
from datetime import datetime
from django.utils.timezone import utc
from django.db import migrations

def add_aggregate_sample_period(apps, schema_editor):
    """Adds a sample period row that represents an aggregate"""
    sample_period = apps.get_model('transit_indicators', 'SamplePeriod')

    # Couldn't find a min/max datetime in python. These values won't be used for anything though.
    start = datetime(1970, 1, 1, 0, 0, 0, tzinfo=utc)
    end = datetime(2070, 1, 1, 0, 0, 0, tzinfo=utc)
    sample_period.objects.create(type='alltime', period_start=start, period_end=end)

class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0015_indicator_the_geom'),
    ]

    operations = [
        migrations.RunPython(add_aggregate_sample_period),
    ]
