# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('gtfs_realtime', '0001_initial'),
    ]

    operations = [
        migrations.RenameField(
            model_name='realstoptime',
            old_name='drop_off_type',
            new_name='dropoff_type',
        ),
        migrations.AlterField(
            model_name='realstoptime',
            name='arrival_time',
            field=models.CharField(max_length=12),
        ),
        migrations.AlterField(
            model_name='realstoptime',
            name='departure_time',
            field=models.CharField(max_length=12),
        ),
    ]
