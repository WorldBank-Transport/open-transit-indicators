# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0011_auto_20140806_1413'),
    ]

    operations = [
        migrations.AlterField(
            model_name='sampleperiod',
            name='type',
            field=models.CharField(unique=True, max_length=7, choices=[(b'morning', b'Morning Rush'), (b'midday', b'Mid Day'), (b'evening', b'Evening Rush'), (b'night', b'Night'), (b'weekend', b'Weekend')]),
        ),
    ]
