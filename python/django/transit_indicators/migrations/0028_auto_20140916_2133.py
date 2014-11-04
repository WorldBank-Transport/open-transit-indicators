# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0027_auto_20140916_2114'),
    ]

    operations = [
        migrations.AlterUniqueTogether(
            name='indicator',
            unique_together=None
        ),
        migrations.RemoveField(
            model_name='indicator',
            name='city_name',
        ),
        migrations.AlterUniqueTogether(
            name='indicator',
            unique_together=set([('sample_period', 'type', 'aggregation', 'route_id', 'route_type', 'version')]),
        ),
    ]
