# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('datasources', '0007_auto_20140723_1342'),
        ('transit_indicators', '0004_auto_20140722_2213'),
    ]

    operations = [
        migrations.CreateModel(
            name='OTIDemographicConfig',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('pop_metric_1_label', models.CharField(max_length=255, null=True, blank=True)),
                ('pop_metric_2_label', models.CharField(max_length=255, null=True, blank=True)),
                ('dest_metric_1_label', models.CharField(max_length=255, null=True, blank=True)),
                ('datasource', models.ForeignKey(to='datasources.DemographicDataSource')),
                ('dest_metric_1_field', models.ForeignKey(blank=True, to='datasources.DemographicDataFieldName', null=True)),
                ('pop_metric_1_field', models.ForeignKey(blank=True, to='datasources.DemographicDataFieldName', null=True)),
                ('pop_metric_2_field', models.ForeignKey(blank=True, to='datasources.DemographicDataFieldName', null=True)),
            ],
            options={
            },
            bases=(models.Model,),
        ),
        migrations.RemoveField(
            model_name='otiindicatorsconfig',
            name='dest_metric_1_field',
        ),
        migrations.RemoveField(
            model_name='otiindicatorsconfig',
            name='dest_metric_1_label',
        ),
        migrations.RemoveField(
            model_name='otiindicatorsconfig',
            name='pop_metric_1_field',
        ),
        migrations.RemoveField(
            model_name='otiindicatorsconfig',
            name='pop_metric_1_label',
        ),
        migrations.RemoveField(
            model_name='otiindicatorsconfig',
            name='pop_metric_2_field',
        ),
        migrations.RemoveField(
            model_name='otiindicatorsconfig',
            name='pop_metric_2_label',
        ),
    ]
