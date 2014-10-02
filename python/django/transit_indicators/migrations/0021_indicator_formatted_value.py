# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0020_auto_20140828_1355'),
    ]

    operations = [
        migrations.AddField(
            model_name='indicator',
            name='formatted_value',
            field=models.CharField(max_length=255, null=True),
            preserve_default=True,
        ),
    ]
