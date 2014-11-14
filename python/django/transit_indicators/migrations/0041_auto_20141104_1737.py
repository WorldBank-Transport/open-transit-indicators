# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0040_migrate_version_data'),
    ]

    operations = [
        migrations.AddField(
            model_name='otiindicatorsconfig',
            name='arrive_by_time_s',
            field=models.PositiveIntegerField(default=0),
            preserve_default=False,
        ),
        migrations.AlterUniqueTogether(
            name='indicator',
            unique_together=set([('sample_period', 'type', 'aggregation', 'route_id', 'route_type', 'calculation_job')]),
        ),
    ]
