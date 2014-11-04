# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations
import django.contrib.gis.db.models.fields


class Migration(migrations.Migration):

    dependencies = [
        ('datasources', '0005_boundary_boundaryproblem'),
    ]

    operations = [
        migrations.CreateModel(
            name='DemographicDataFeature',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('create_date', models.DateTimeField(auto_now_add=True)),
                ('last_modify_date', models.DateTimeField(auto_now=True)),
                ('population_metric_1', models.FloatField(null=True, blank=True)),
                ('population_metric_2', models.FloatField(null=True, blank=True)),
                ('destination_metric_1', models.FloatField(null=True, blank=True)),
                ('geom', django.contrib.gis.db.models.fields.MultiPolygonField(srid=3857)),
            ],
            options={
                'abstract': False,
            },
            bases=(models.Model,),
        ),
        migrations.CreateModel(
            name='DemographicDataFieldName',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('name', models.CharField(max_length=10)),
            ],
            options={
            },
            bases=(models.Model,),
        ),
        migrations.CreateModel(
            name='DemographicDataSource',
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
        migrations.AddField(
            model_name='demographicdatafieldname',
            name='datasource',
            field=models.ForeignKey(to='datasources.DemographicDataSource'),
            preserve_default=True,
        ),
        migrations.CreateModel(
            name='DemographicDataSourceProblem',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('type', models.CharField(max_length=3, choices=[(b'err', b'Error'), (b'war', b'Warning')])),
                ('description', models.TextField()),
                ('title', models.CharField(max_length=255)),
                ('source_file', models.ForeignKey(to='datasources.DemographicDataSource')),
            ],
            options={
                'abstract': False,
            },
            bases=(models.Model,),
        ),
    ]
