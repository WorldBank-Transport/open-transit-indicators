# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('datasources', '0008_auto_20140723_1804'),
    ]

    operations = [
        migrations.CreateModel(
            name='OSMData',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('create_date', models.DateTimeField(auto_now_add=True)),
                ('last_modify_date', models.DateTimeField(auto_now=True)),
                ('is_downloaded', models.BooleanField(default=False)),
                ('source_file', models.FileField(upload_to=b'')),
                ('is_valid', models.NullBooleanField()),
                ('is_processed', models.BooleanField(default=False)),
                ('gtfsfeed', models.ForeignKey(to='datasources.GTFSFeed')),
            ],
            options={
                'abstract': False,
            },
            bases=(models.Model,),
        ),
        migrations.CreateModel(
            name='OSMDataProblem',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('type', models.CharField(max_length=3, choices=[(b'err', b'Error'), (b'war', b'Warning')])),
                ('description', models.TextField()),
                ('title', models.CharField(max_length=255)),
                ('osmdata', models.ForeignKey(to='datasources.OSMData')),
            ],
            options={
                'abstract': False,
            },
            bases=(models.Model,),
        ),
    ]
