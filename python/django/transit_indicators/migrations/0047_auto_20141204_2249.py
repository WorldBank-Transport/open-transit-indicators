# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0046_auto_20141201_1515'),
    ]

    operations = [
        migrations.AlterField(
            model_name='scenario',
            name='base_scenario',
            field=models.ForeignKey(to_field=b'db_name', blank=True, to='transit_indicators.Scenario', null=True),
        ),
    ]
