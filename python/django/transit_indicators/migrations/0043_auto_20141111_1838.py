# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0042_merge'),
    ]

    operations = [
        migrations.AlterField(
            model_name='indicator',
            name='type',
            field=models.CharField(max_length=32, choices=[(b'access_index', 'Access index'), (b'affordability', 'Affordability'), (b'avg_service_freq', 'Average Service Frequency'), (b'coverage_ratio_stops_buffer', 'Coverage of transit stops'), (b'distance_stops', 'Distance between stops'), (b'dwell_time', 'Dwell Time Performance'), (b'hours_service', 'Weekly number of hours of service'), (b'job_access', 'Job accessibility'), (b'length', 'Transit system length'), (b'lines_roads', 'Ratio of transit lines length over road length'), (b'line_network_density', 'Transit line network density'), (b'num_modes', 'Number of modes'), (b'num_routes', 'Number of routes'), (b'num_stops', 'Number of stops'), (b'num_types', 'Number of route types'), (b'on_time_perf', 'On-Time Performance'), (b'regularity_headways', 'Regularity of Headways'), (b'service_freq_weighted', 'Service frequency weighted by served population'), (b'service_freq_weighted_low', 'Service frequency weighted by served low-income population'), (b'stops_route_length', 'Ratio of number of stops to route-length'), (b'ratio_suburban_lines', 'Ratio of the Transit-Pattern Operating Suburban Lines'), (b'system_access', 'System accessibility'), (b'system_access_low', 'System accessibility - low-income'), (b'time_traveled_stops', 'Time traveled between stops'), (b'travel_time', 'Travel Time Performance'), (b'weekday_end_freq', 'Weekday / weekend frequency'), (b'jobs_travelshed', 'Number of jobs that can be reached by an area.')]),
        ),
    ]
