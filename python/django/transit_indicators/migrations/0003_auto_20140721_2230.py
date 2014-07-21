# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0002_auto_20140721_2121'),
    ]

    operations = [
        migrations.AddField(
            model_name='otiindicatorsconfig',
            name='city_boundary',
            field=models.ForeignKey(blank=True, to='datasources.Boundary', null=True),
            preserve_default=True,
        ),
        migrations.AddField(
            model_name='otiindicatorsconfig',
            name='region_boundary',
            field=models.ForeignKey(blank=True, to='datasources.Boundary', null=True),
            preserve_default=True,
        ),
    ]
