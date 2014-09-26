# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0008_sampleperiod'),
    ]

    operations = [
        migrations.DeleteModel(
            name='PeakTravelPeriod',
        ),
    ]
