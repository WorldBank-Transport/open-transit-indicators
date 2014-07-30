# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations

def add_global_config(apps, schema_editor):
    """ Creates an OTIIndicatorsConfig row. This is done as a data migration,
    because there should only ever be a single row in the database used
    for this. It represents the global configuration for indicator calculations.
    The rest of the app should be able to assume that this single row exists
    and can update it as needed.
    """
    config = apps.get_model('transit_indicators', 'OTIIndicatorsConfig')
    config.objects.create(poverty_line=0, avg_fare=0, nearby_buffer_distance_m=0,
                          max_commute_time_s=0, max_walk_time_s=0)

class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0006_auto_20140723_1806'),
    ]

    operations = [
        migrations.RunPython(add_global_config),
    ]
