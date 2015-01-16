# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0003_auto_20140722_1256'),
        ('datasources', '0006_auto_20140722_2213'),
    ]

    operations = [
        migrations.AddField(
            model_name='otiindicatorsconfig',
            name='dest_metric_1_field',
            field=models.ForeignKey(blank=True, to='datasources.DemographicDataFieldName', null=True),
            preserve_default=True,
        ),
        migrations.AddField(
            model_name='otiindicatorsconfig',
            name='dest_metric_1_label',
            field=models.CharField(max_length=255, null=True, blank=True),
            preserve_default=True,
        ),
        migrations.AddField(
            model_name='otiindicatorsconfig',
            name='pop_metric_1_field',
            field=models.ForeignKey(blank=True, to='datasources.DemographicDataFieldName', null=True),
            preserve_default=True,
        ),
        migrations.AddField(
            model_name='otiindicatorsconfig',
            name='pop_metric_1_label',
            field=models.CharField(max_length=255, null=True, blank=True),
            preserve_default=True,
        ),
        migrations.AddField(
            model_name='otiindicatorsconfig',
            name='pop_metric_2_field',
            field=models.ForeignKey(blank=True, to='datasources.DemographicDataFieldName', null=True),
            preserve_default=True,
        ),
        migrations.AddField(
            model_name='otiindicatorsconfig',
            name='pop_metric_2_label',
            field=models.CharField(max_length=255, null=True, blank=True),
            preserve_default=True,
        ),
    ]
