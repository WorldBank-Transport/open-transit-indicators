# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0018_auto_20140826_2045'),
    ]

    operations = [
        migrations.AlterField(
            model_name='indicator',
            name='indicator_job',
            field=models.ForeignKey(to='transit_indicators.IndicatorJob', to_field=b'version'),
        ),
    ]
