# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0010_indicator'),
    ]

    operations = [
        migrations.AlterField(
            model_name='otiindicatorsconfig',
            name='city_boundary',
            field=models.ForeignKey(on_delete=django.db.models.deletion.SET_NULL, blank=True, to='datasources.Boundary', null=True),
        ),
        migrations.AlterField(
            model_name='otiindicatorsconfig',
            name='region_boundary',
            field=models.ForeignKey(on_delete=django.db.models.deletion.SET_NULL, blank=True, to='datasources.Boundary', null=True),
        ),
    ]
