# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0026_auto_20140911_2016'),
    ]

    operations = [
        migrations.AddField(
            model_name='indicatorjob',
            name='city_name',
            field=models.CharField(default=b'My City', max_length=255),
            preserve_default=True,
        ),
    ]
