# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('datasources', '0009_osmdata_osmdataproblem'),
    ]

    operations = [
        migrations.AddField(
            model_name='boundary',
            name='city_name',
            field=models.CharField(default='My City', max_length=255),
            preserve_default=True,
        ),
        migrations.AddField(
            model_name='demographicdatafeature',
            name='city_name',
            field=models.CharField(default='My City', max_length=255),
            preserve_default=True,
        ),
        migrations.AddField(
            model_name='demographicdatasource',
            name='city_name',
            field=models.CharField(default='My City', max_length=255),
            preserve_default=True,
        ),
        migrations.AddField(
            model_name='gtfsfeed',
            name='city_name',
            field=models.CharField(default='My City', max_length=255),
            preserve_default=True,
        ),
        migrations.AddField(
            model_name='osmdata',
            name='city_name',
            field=models.CharField(default='My City', max_length=255),
            preserve_default=True,
        ),
    ]
