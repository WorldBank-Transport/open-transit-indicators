# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0013_indicator_city_name'),
    ]

    operations = [
        migrations.AlterUniqueTogether(
            name='indicator',
            unique_together=set([('sample_period', 'type', 'aggregation', 'route_id', 'route_type', 'city_name', 'version')]),
        ),
    ]
