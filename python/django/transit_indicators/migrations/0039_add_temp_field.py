# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations

class Migration(migrations.Migration):
    """This annoyingly small migration is necessary to carry out the data migration in 0040"""
    dependencies = [
        ('transit_indicators', '0038_oticityname'),
    ]

    operations = [
        migrations.AddField(
            model_name='indicator',
            name='temp',
            field=models.ForeignKey(to='transit_indicators.IndicatorJob')
        )
    ]
