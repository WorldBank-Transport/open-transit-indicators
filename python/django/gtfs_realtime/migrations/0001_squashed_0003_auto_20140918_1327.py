# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    replaces = [(b'gtfs_realtime', '0001_initial'), (b'gtfs_realtime', '0002_auto_20140918_1321'), (b'gtfs_realtime', '0003_auto_20140918_1327')]

    dependencies = [
        ('datasources', '0012_realtime_city_name'),
    ]

    operations = [
        migrations.CreateModel(
            name='RealStopTime',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('trip_id', models.CharField(max_length=255)),
                ('stop_id', models.CharField(max_length=255)),
                ('stop_sequence', models.IntegerField()),
                ('arrival_time', models.CharField(max_length=12)),
                ('departure_time', models.CharField(max_length=12)),
                ('stop_headsign', models.CharField(max_length=255, null=True, blank=True)),
                ('pickup_type', models.IntegerField(default=0)),
                ('drop_off_type', models.IntegerField(default=0)),
                ('shape_dist_traveled', models.FloatField(null=True, blank=True)),
                ('datasource', models.ForeignKey(to='datasources.RealTime')),
            ],
            options={
                'abstract': False,
                'db_table': 'gtfs_stop_times_real',
            },
            bases=(models.Model,),
        ),
    ]
