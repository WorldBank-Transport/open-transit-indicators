# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0044_remove_otiindicatorsconfig_max_walk_time_s'),
    ]

    operations = [
        migrations.AddField(
            model_name='indicatorjob',
            name='error_type',
            field=models.CharField(default=b'', max_length=20, null=True, blank=True, choices=[(b'', 'None applicable'), (b'scala_unknown_error', 'Scala died during processing')]),
            preserve_default=True,
        ),
    ]
