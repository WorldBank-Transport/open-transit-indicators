# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    replaces = [(b'transit_indicators', '0001_initial'), (b'transit_indicators', '0002_remove_peaktravelperiod_indicator_config')]

    dependencies = [
    ]

    operations = [
        migrations.CreateModel(
            name='OTIIndicatorsConfig',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('poverty_line_usd', models.FloatField()),
                ('nearby_buffer_distance_m', models.FloatField()),
                ('max_commute_time_s', models.IntegerField()),
                ('max_walk_time_s', models.IntegerField()),
            ],
            options={
            },
            bases=(models.Model,),
        ),
        migrations.CreateModel(
            name='PeakTravelPeriod',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('start_time', models.TimeField()),
                ('end_time', models.TimeField()),
            ],
            options={
            },
            bases=(models.Model,),
        ),
    ]
