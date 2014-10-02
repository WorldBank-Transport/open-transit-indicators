# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations
import django.contrib.gis.db.models.fields


class Migration(migrations.Migration):

    dependencies = [
        ('datasources', '0004_auto_20140415_1325'),
    ]

    operations = [
        migrations.CreateModel(
            name='Boundary',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('create_date', models.DateTimeField(auto_now_add=True)),
                ('last_modify_date', models.DateTimeField(auto_now=True)),
                ('source_file', models.FileField(upload_to=b'')),
                ('is_valid', models.NullBooleanField()),
                ('is_processed', models.BooleanField(default=False)),
                ('geom', django.contrib.gis.db.models.fields.MultiPolygonField(srid=3857, null=True, blank=True)),
            ],
            options={
                'abstract': False,
            },
            bases=(models.Model,),
        ),
        migrations.CreateModel(
            name='BoundaryProblem',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('type', models.CharField(max_length=3, choices=[(b'err', b'Error'), (b'war', b'Warning')])),
                ('description', models.TextField()),
                ('title', models.CharField(max_length=255)),
                ('boundary', models.ForeignKey(to='datasources.Boundary')),
            ],
            options={
                'abstract': False,
            },
            bases=(models.Model,),
        ),
    ]
