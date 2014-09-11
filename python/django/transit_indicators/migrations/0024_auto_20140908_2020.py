# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0023_auto_20140904_1922'),
    ]

    operations = [
        migrations.AlterField(
            model_name='otidemographicconfig',
            name='dest_metric_1_field',
            field=models.ForeignKey(related_name=b'+', blank=True, to='datasources.DemographicDataFieldName', null=True),
        ),
        migrations.AlterField(
            model_name='otidemographicconfig',
            name='pop_metric_1_field',
            field=models.ForeignKey(related_name=b'+', blank=True, to='datasources.DemographicDataFieldName', null=True),
        ),
        migrations.AlterField(
            model_name='otidemographicconfig',
            name='pop_metric_2_field',
            field=models.ForeignKey(related_name=b'+', blank=True, to='datasources.DemographicDataFieldName', null=True),
        ),
        migrations.AlterField(
            model_name='otiindicatorsconfig',
            name='city_boundary',
            field=models.ForeignKey(related_name=b'+', on_delete=django.db.models.deletion.SET_NULL, blank=True, to='datasources.Boundary', null=True),
        ),
        migrations.AlterField(
            model_name='otiindicatorsconfig',
            name='region_boundary',
            field=models.ForeignKey(related_name=b'+', on_delete=django.db.models.deletion.SET_NULL, blank=True, to='datasources.Boundary', null=True),
        ),
    ]
