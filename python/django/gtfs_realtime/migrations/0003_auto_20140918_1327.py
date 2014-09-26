# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('gtfs_realtime', '0002_auto_20140918_1321'),
    ]

    operations = [
        migrations.RenameField(
            model_name='realstoptime',
            old_name='dropoff_type',
            new_name='drop_off_type',
        ),
    ]
