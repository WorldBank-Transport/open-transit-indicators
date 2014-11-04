# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0040_migrate_version_data'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='indicator',
            name='formatted_value',
        ),
    ]
