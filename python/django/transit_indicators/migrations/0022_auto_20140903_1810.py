# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0021_auto_20140902_1846'),
    ]

    operations = [
        migrations.AddField(
            model_name='indicator',
            name='formatted_value',
            field=models.CharField(max_length=255, null=True),
            preserve_default=True,
        )
    ]
