# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0001_squashed_0002_remove_peaktravelperiod_indicator_config'),
    ]

    operations = [
        migrations.AlterField(
            model_name='otiindicatorsconfig',
            name='max_commute_time_s',
            field=models.PositiveIntegerField(),
        ),
        migrations.AlterField(
            model_name='otiindicatorsconfig',
            name='max_walk_time_s',
            field=models.PositiveIntegerField(),
        ),
    ]
