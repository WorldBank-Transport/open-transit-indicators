# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations
import transit_indicators.models


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0037_gtfsroute'),
    ]

    operations = [
        migrations.CreateModel(
            name='OTICityName',
            fields=[
                ('city_name', models.CharField(max_length=255, serialize=False, primary_key=True)),
            ],
            options={
            },
            bases=(models.Model,),
        ),
        migrations.AlterField(
            model_name='indicatorjob',
            name='city_name',
            field=models.CharField(default=transit_indicators.models.get_city_name, max_length=255),
        ),
    ]
