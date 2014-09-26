# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('datasources', '0013_auto_20140924_1550'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='demographicdatasource',
            name='is_loaded',
        ),
    ]
