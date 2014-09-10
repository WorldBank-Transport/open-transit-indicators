# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0019_auto_20140828_1312'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='indicator',
            name='indicator_job',
        ),
        migrations.AlterField(
            model_name='indicator',
            name='version',
            field=models.ForeignKey(to='transit_indicators.IndicatorJob', to_field=b'version'),
        ),
    ]
