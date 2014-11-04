# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations
import datetime


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0032_scenario'),
    ]

    operations = [
        migrations.AddField(
            model_name='scenario',
            name='create_date',
            field=models.DateTimeField(default=datetime.datetime.now, auto_now_add=True),
            preserve_default=True,
        ),
        migrations.AddField(
            model_name='scenario',
            name='last_modify_date',
            field=models.DateTimeField(default=datetime.datetime.now, auto_now=True),
            preserve_default=True,
        ),
    ]
