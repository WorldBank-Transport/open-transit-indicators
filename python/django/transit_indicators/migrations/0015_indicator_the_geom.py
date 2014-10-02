# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations
import django.contrib.gis.db.models.fields


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0014_auto_20140808_1756'),
    ]

    operations = [
        migrations.AddField(
            model_name='indicator',
            name='the_geom',
            field=django.contrib.gis.db.models.fields.GeometryField(srid=4326, null=True),
            preserve_default=True,
        ),
    ]
