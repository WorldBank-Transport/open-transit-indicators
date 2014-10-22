# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0034_indicatorjob_scenario'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='indicatorjob',
            name='sample_periods',
        ),
    ]
