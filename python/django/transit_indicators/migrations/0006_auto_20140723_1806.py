# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0005_auto_20140723_1342'),
    ]

    operations = [
        migrations.AddField(
            model_name='otiindicatorsconfig',
            name='avg_fare',
            field=models.FloatField(default=0),
            preserve_default=False,
        ),
        migrations.RenameField(
            model_name='otiindicatorsconfig',
            old_name='poverty_line_usd',
            new_name='poverty_line',
        ),
    ]
