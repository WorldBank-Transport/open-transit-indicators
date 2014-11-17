# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0043_auto_20141111_1838'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='otiindicatorsconfig',
            name='max_walk_time_s',
        ),
    ]
