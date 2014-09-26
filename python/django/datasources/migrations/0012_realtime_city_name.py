# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('datasources', '0011_merge'),
    ]

    operations = [
        migrations.AddField(
            model_name='realtime',
            name='city_name',
            field=models.CharField(default=b'My City', max_length=255),
            preserve_default=True,
        ),
    ]
