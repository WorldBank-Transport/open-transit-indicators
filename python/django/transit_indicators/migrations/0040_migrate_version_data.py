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
        indicator.calculation_job = indicator.temp
        indicator.save()

class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0039_add_temp_field'),
    ]

    operations = [
        migrations.RunPython(
            uuid_to_id
        ),
        migrations.AlterField(
            model_name='indicator',
            name='type',
            field=models.CharField(max_length=32, choices=[(b'access_index', 'Access index'), (b'affordability', 'Affordability'), (b'avg_service_freq', 'Average Service Frequency'), (b'coverage_ratio_stops_buffer', 'Coverage of transit stops'), (b'distance_stops', 'Distance between stops'), (b'dwell_time', 'Dwell Time Performance'), (b'hours_service', 'Weekly number of hours of service'), (b'job_access', 'Job accessibility'), (b'length', 'Transit system length'), (b'lines_roads', 'Ratio of transit lines length over road length'), (b'line_network_density', 'Transit line network density'), (b'num_modes', 'Number of modes'), (b'num_routes', 'Number of routes'), (b'num_stops', 'Number of stops'), (b'num_types', 'Number of route types'), (b'on_time_perf', 'On-Time Performance'), (b'regularity_headways', 'Regularity of Headways'), (b'service_freq_weighted', 'Service frequency weighted by served population'), (b'service_freq_weighted_low', 'Service frequency weighted by served low-income population'), (b'stops_route_length', 'Ratio of number of stops to route-length'), (b'ratio_suburban_lines', 'Ratio of the Transit-Pattern Operating Suburban Lines'), (b'system_access', 'System accessibility'), (b'system_access_low', 'System accessibility - low-income'), (b'time_traveled_stops', 'Time traveled between stops'), (b'travel_time', 'Travel Time Performance'), (b'weekday_end_freq', 'Weekday / weekend frequency')]),
        ),
        migrations.AddField(
            model_name='indicator',
            name='calculation_job',
            field=models.ForeignKey(to='transit_indicators.IndicatorJob')
        ),
        migrations.AlterUniqueTogether(
            name='indicator',
            unique_together=set([('sample_period', 'type', 'aggregation', 'route_id', 'route_type', 'calculation_job')]),
        ),
        migrations.RemoveField(
            model_name='indicator',
            name='version',
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
