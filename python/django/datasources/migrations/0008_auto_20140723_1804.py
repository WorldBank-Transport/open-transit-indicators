# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('datasources', '0007_auto_20140723_1342'),
    ]

    operations = [
        migrations.RenameField(
            model_name='demographicdatasourceproblem',
            old_name='source_file',
            new_name='datasource',
        ),
        migrations.AlterField(
            model_name='demographicdatafieldname',
            name='name',
            field=models.CharField(max_length=10, db_index=True),
        ),
    ]
