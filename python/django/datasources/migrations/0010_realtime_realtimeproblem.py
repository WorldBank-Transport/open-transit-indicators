# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('datasources', '0009_osmdata_osmdataproblem'),
    ]

    operations = [
        migrations.CreateModel(
            name='RealTime',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('create_date', models.DateTimeField(auto_now_add=True)),
                ('last_modify_date', models.DateTimeField(auto_now=True)),
                ('source_file', models.FileField(upload_to=b'')),
                ('is_valid', models.NullBooleanField()),
                ('is_processed', models.BooleanField(default=False)),
            ],
            options={
                'abstract': False,
            },
            bases=(models.Model,),
        ),
        migrations.CreateModel(
            name='RealTimeProblem',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('type', models.CharField(max_length=3, choices=[(b'err', 'Error'), (b'war', 'Warning')])),
                ('description', models.TextField()),
                ('title', models.CharField(max_length=255)),
                ('realtime', models.ForeignKey(to='datasources.RealTime')),
            ],
            options={
                'abstract': False,
            },
            bases=(models.Model,),
        ),
    ]
