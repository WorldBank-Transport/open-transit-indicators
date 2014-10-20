# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations
from django.conf import settings
import uuid


class Migration(migrations.Migration):

    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
        ('transit_indicators', '0031_indicatorjob_calculation_status'),
    ]

    operations = [
        migrations.CreateModel(
            name='Scenario',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('db_name', models.CharField(default=uuid.uuid4, unique=True, max_length=40)),
                ('job_status', models.CharField(max_length=10, choices=[(b'queued', 'Scenario initialization queued for processing'), (b'processing', 'Scenario initialization is processing'), (b'error', 'Error initializing scenario'), (b'complete', 'Completed scenario initialization')])),
                ('name', models.CharField(max_length=50)),
                ('description', models.CharField(max_length=255, null=True, blank=True)),
                ('base_scenario', models.ForeignKey(blank=True, to='transit_indicators.Scenario', null=True)),
                ('created_by', models.ForeignKey(to=settings.AUTH_USER_MODEL)),
                ('sample_period', models.ForeignKey(to='transit_indicators.SamplePeriod')),
            ],
            options={
            },
            bases=(models.Model,),
        ),
    ]
