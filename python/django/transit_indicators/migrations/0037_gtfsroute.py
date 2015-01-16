# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0036_update_indicator_choices'),
    ]

    operations = [
        migrations.CreateModel(
            name='GTFSRoute',
            fields=[
            ],
            options={
                'db_table': 'gtfs_routes',
                'managed': False,
            },
            bases=(models.Model,),
        ),
    ]
