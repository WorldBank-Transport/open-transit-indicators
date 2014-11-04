# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations
from django.conf import settings


def create_initial_indicator_job(apps, schema_editor):
    try:
        IndicatorJob = apps.get_model("transit_indicators", "IndicatorJob")
        OTIUser = apps.get_model("userdata", "OTIUser")
        admin = OTIUser.objects.get(username="oti-admin")
        IndicatorJob.objects.create(job_status="complete", version=1, payload="{}", created_by=admin)
    except OTIUser.DoesNotExist:
        pass

class Migration(migrations.Migration):

    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
        ('transit_indicators', '0017_add_alltime_sample_period'),
    ]

    operations = [
        migrations.CreateModel(
            name='IndicatorJob',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('job_status', models.CharField(max_length=10, choices=[(b'queued', b'Job queued for processing'), (b'processing', b'Indicators being processed and calculated'), (b'error', b'Error calculating indicators'), (b'complete', b'Completed indicator calculation')])),
                ('version', models.IntegerField(unique=True)),
                ('payload', models.TextField()),
                ('created_by', models.ForeignKey(to=settings.AUTH_USER_MODEL)),
                ('sample_periods', models.ManyToManyField(to='transit_indicators.SamplePeriod')),
            ],
            options={
            },
            bases=(models.Model,),
        ),
        migrations.RunPython(create_initial_indicator_job),
        migrations.AddField(
            model_name='indicator',
            name='indicator_job',
            field=models.ForeignKey(default=1, to='transit_indicators.IndicatorJob'),
            preserve_default=False,
        ),
    ]
