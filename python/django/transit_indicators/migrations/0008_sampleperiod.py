# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0007_add_global_config_row'),
    ]

    operations = [
        migrations.CreateModel(
            name='SamplePeriod',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('type', models.CharField(max_length=7, choices=[(b'morning', b'Morning Rush'), (b'midday', b'Mid Day'), (b'evening', b'Evening Rush'), (b'night', b'Night'), (b'weekend', b'Weekend')])),
                ('period_start', models.DateTimeField()),
                ('period_end', models.DateTimeField()),
            ],
            options={
            },
            bases=(models.Model,),
        ),
    ]
