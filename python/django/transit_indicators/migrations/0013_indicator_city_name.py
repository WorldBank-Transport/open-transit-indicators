# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0012_auto_20140806_1521'),
    ]

    operations = [
        migrations.AddField(
            model_name='indicator',
            name='city_name',
            field=models.CharField(max_length=255, null=True),
            preserve_default=True,
        ),
    ]
