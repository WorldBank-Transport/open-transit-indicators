# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0025_auto_20140908_2041'),
    ]

    operations = [
        migrations.AlterField(
            model_name='indicator',
            name='city_name',
            field=models.CharField(default='My City', max_length=255),
        ),
    ]
