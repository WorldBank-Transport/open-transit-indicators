# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0033_add_create_and_modify_dts_to_scenario'),
    ]

    operations = [
        migrations.AddField(
            model_name='indicatorjob',
            name='scenario',
            field=models.ForeignKey(blank=True, to='transit_indicators.Scenario', null=True),
            preserve_default=True,
        ),
    ]
