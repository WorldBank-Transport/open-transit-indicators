# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations

def uuid_to_id(apps, schema_editor):
    """This function based on the data migration function demonstrated at
    https://docs.djangoproject.com/en/1.7/topics/migrations/#data-migrations"""
    IndicatorJob = apps.get_model('transit_indicators', 'IndicatorJob')
    Indicator = apps.get_model('transit_indicators', 'Indicator')
    for indicator in Indicator.objects.all():
        indicator.temp = IndicatorJob.objects.filter(version=indicator.version)[0]
        indicator.save()

def copy_column(apps, schema_editor):
    """This function copies a column into another column"""
    Indicator = apps.get_model('transit_indicators', 'Indicator')
    for indicator in Indicator.objects.all():
        indicator.version = indicator.temp
        indicator.save()

class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0039_add_temp_field'),
    ]

    operations = [
        migrations.RunPython(
            uuid_to_id
        ),
        migrations.RemoveField(
            model_name='indicator',
            name='version',
        ),
        migrations.AddField(
            model_name='indicator',
            name='version',
            field=models.ForeignKey(to='transit_indicators.IndicatorJob')
        ),
        migrations.RunPython(
            copy_column
        ),
        migrations.RemoveField(
            model_name='indicator',
            name='temp',
        ),
        migrations.RemoveField(
            model_name='indicatorjob',
            name='is_latest_version',
        ),
        migrations.RemoveField(
            model_name='indicatorjob',
            name='version',
        ),
    ]
