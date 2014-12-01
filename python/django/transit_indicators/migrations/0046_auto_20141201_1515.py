# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0045_indicatorjob_error_type'),
    ]

    operations = [
        migrations.AlterField(
            model_name='sampleperiod',
            name='type',
            field=models.CharField(unique=True, max_length=7, choices=[(b'alltime', 'All Time'), (b'morning', 'Morning Peak'), (b'midday', 'Mid Day'), (b'evening', 'Evening Peak'), (b'night', 'Night'), (b'weekend', 'Weekend')]),
        ),
    ]
