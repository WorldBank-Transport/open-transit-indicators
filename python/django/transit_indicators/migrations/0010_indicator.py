# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0009_delete_peaktravelperiod'),
    ]

    operations = [
        migrations.CreateModel(
            name='Indicator',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('type', models.CharField(max_length=32, choices=[(b'access_index', b'Access index'), (b'affordability', b'Affordability'), (b'avg_service_freq', b'Average Service Frequency'), (b'coverage', b'System coverage'), (b'coverage_stops', b'Coverage of transit stops'), (b'distance_stops', b'Distance between stops'), (b'dwell_time', b'Dwell Time Performance'), (b'hours_service', b'Weekly number of hours of service'), (b'job_access', b'Job accessibility'), (b'length', b'Transit system length'), (b'lines_roads', b'Ratio of transit lines length over road length'), (b'line_network_density', b'Transit line network density'), (b'num_modes', b'Number of modes'), (b'num_routes', b'Number of routes'), (b'num_stops', b'Number of stops'), (b'num_types', b'Number of route types'), (b'on_time_perf', b'On-Time Performance'), (b'regularity_headways', b'Regularity of Headways'), (b'service_freq_weighted', b'Service frequency weighted by served population'), (b'stops_route_length', b'Ratio of number of stops to route-length'), (b'suburban_lines', b'Ratio of the Transit-Pattern Operating Suburban Lines'), (b'system_access', b'System accessibility'), (b'system_access_low', b'System accessibility - low-income'), (b'time_traveled_stops', b'Time traveled between stops'), (b'travel_time', b'Travel Time Performance'), (b'weekday_end_freq', b'Weekday / weekend frequency')])),
                ('aggregation', models.CharField(max_length=6, choices=[(b'route', b'Route'), (b'mode', b'Mode'), (b'system', b'System')])),
                ('route_id', models.CharField(max_length=32, null=True)),
                ('route_type', models.PositiveIntegerField(null=True)),
                ('city_bounded', models.BooleanField(default=False)),
                ('version', models.PositiveIntegerField(default=0)),
                ('value', models.FloatField(default=0)),
                ('sample_period', models.ForeignKey(to='transit_indicators.SamplePeriod')),
            ],
            options={
            },
            bases=(models.Model,),
        ),
    ]
