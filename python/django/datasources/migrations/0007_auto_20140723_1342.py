# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('datasources', '0006_auto_20140722_2213'),
    ]

    operations = [
        migrations.AddField(
            model_name='demographicdatafeature',
            name='datasource',
            field=models.ForeignKey(default=1, to='datasources.DemographicDataSource'),
            preserve_default=False,
        ),
        migrations.AddField(
            model_name='demographicdatasource',
            name='is_loaded',
            field=models.BooleanField(default=False),
            preserve_default=True,
        ),
        migrations.AddField(
            model_name='demographicdatasource',
            name='num_features',
            field=models.PositiveIntegerField(null=True, blank=True),
            preserve_default=True,
        ),
    ]
