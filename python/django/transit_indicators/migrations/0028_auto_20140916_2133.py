# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0027_auto_20140916_2114'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='indicator',
            name='city_name',
        ),
    ]
