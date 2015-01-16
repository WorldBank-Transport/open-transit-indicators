# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations
import uuid


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0024_auto_20140908_2020'),
    ]

    operations = [
        migrations.AlterUniqueTogether(
            name='indicator',
            unique_together=None
        ),
        migrations.AlterField(
            model_name='indicatorjob',
            name='version',
            field=models.CharField(default=uuid.uuid4, unique=True, max_length=40),
        ),
        migrations.RemoveField(
            model_name='indicatorjob',
            name='payload',
        ),
        migrations.AddField(
            model_name='indicatorjob',
            name='is_latest_version',
            field=models.BooleanField(default=False),
            preserve_default=True,
        ),
        migrations.RemoveField(
            model_name='indicator',
            name='version',
        ),
        migrations.AddField(
            model_name='indicator',
            name='version',
            field=models.ForeignKey(to='transit_indicators.IndicatorJob', to_field=b'version'),
            preserve_default=True,
        ),
        migrations.AlterUniqueTogether(
            name='indicator',
            unique_together=set([('sample_period', 'type', 'aggregation', 'route_id', 'route_type', 'city_name', 'version')]),
        ),
    ]
