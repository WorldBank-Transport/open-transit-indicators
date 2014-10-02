# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0016_add_aggregate_sample_period'),
    ]

    operations = [
        migrations.AlterField(
            model_name='sampleperiod',
            name='type',
            field=models.CharField(unique=True, max_length=7, choices=[(b'alltime', b'All Time'), (b'morning', b'Morning Rush'), (b'midday', b'Mid Day'), (b'evening', b'Evening Rush'), (b'night', b'Night'), (b'weekend', b'Weekend')]),
        ),
    ]
