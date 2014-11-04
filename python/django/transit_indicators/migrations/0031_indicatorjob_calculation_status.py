# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0030_remove_gtfsroutetype_description'),
    ]

    operations = [
        migrations.AddField(
            model_name='indicatorjob',
            name='calculation_status',
            field=models.TextField(default=b'{}'),
            preserve_default=True,
        ),
    ]
