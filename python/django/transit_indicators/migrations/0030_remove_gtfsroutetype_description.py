# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0029_auto_20140918_1820'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='gtfsroutetype',
            name='description',
        ),
    ]
