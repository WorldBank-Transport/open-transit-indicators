# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('datasources', '0012_realtime_city_name'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='boundary',
            name='is_processed',
        ),
        migrations.RemoveField(
            model_name='boundary',
            name='is_valid',
        ),
        migrations.RemoveField(
            model_name='demographicdatasource',
            name='is_processed',
        ),
        migrations.RemoveField(
            model_name='demographicdatasource',
            name='is_valid',
        ),
        migrations.RemoveField(
            model_name='gtfsfeed',
            name=b'is_processed',
        ),
        migrations.RemoveField(
            model_name='gtfsfeed',
            name=b'is_valid',
        ),
        migrations.RemoveField(
            model_name='osmdata',
            name='is_downloaded',
        ),
        migrations.RemoveField(
            model_name='osmdata',
            name='is_processed',
        ),
        migrations.RemoveField(
            model_name='osmdata',
            name='is_valid',
        ),
        migrations.RemoveField(
            model_name='realtime',
            name='is_processed',
        ),
        migrations.RemoveField(
            model_name='realtime',
            name='is_valid',
        ),
        migrations.AddField(
            model_name='boundary',
            name='status',
            field=models.CharField(default=b'pending', max_length=16, choices=[(b'pending', 'Pending'), (b'uploading', 'Uploading'), (b'uploaded', 'Uploaded'), (b'processing', 'Processing'), (b'downloading', 'Downloading'), (b'waiting_input', 'Waiting for User Input'), (b'validating', 'Validating'), (b'complete', 'Complete'), (b'warning', 'Warning'), (b'error', 'Error')]),
            preserve_default=True,
        ),
        migrations.AddField(
            model_name='demographicdatasource',
            name='status',
            field=models.CharField(default=b'pending', max_length=16, choices=[(b'pending', 'Pending'), (b'uploading', 'Uploading'), (b'uploaded', 'Uploaded'), (b'processing', 'Processing'), (b'downloading', 'Downloading'), (b'waiting_input', 'Waiting for User Input'), (b'validating', 'Validating'), (b'complete', 'Complete'), (b'warning', 'Warning'), (b'error', 'Error')]),
            preserve_default=True,
        ),
        migrations.AddField(
            model_name='gtfsfeed',
            name='status',
            field=models.CharField(default=b'pending', max_length=16, choices=[(b'pending', 'Pending'), (b'uploading', 'Uploading'), (b'uploaded', 'Uploaded'), (b'processing', 'Processing'), (b'downloading', 'Downloading'), (b'waiting_input', 'Waiting for User Input'), (b'validating', 'Validating'), (b'complete', 'Complete'), (b'warning', 'Warning'), (b'error', 'Error')]),
            preserve_default=True,
        ),
        migrations.AddField(
            model_name='osmdata',
            name='status',
            field=models.CharField(default=b'pending', max_length=16, choices=[(b'pending', 'Pending'), (b'uploading', 'Uploading'), (b'uploaded', 'Uploaded'), (b'processing', 'Processing'), (b'downloading', 'Downloading'), (b'waiting_input', 'Waiting for User Input'), (b'validating', 'Validating'), (b'complete', 'Complete'), (b'warning', 'Warning'), (b'error', 'Error')]),
            preserve_default=True,
        ),
        migrations.AddField(
            model_name='realtime',
            name='status',
            field=models.CharField(default=b'pending', max_length=16, choices=[(b'pending', 'Pending'), (b'uploading', 'Uploading'), (b'uploaded', 'Uploaded'), (b'processing', 'Processing'), (b'downloading', 'Downloading'), (b'waiting_input', 'Waiting for User Input'), (b'validating', 'Validating'), (b'complete', 'Complete'), (b'warning', 'Warning'), (b'error', 'Error')]),
            preserve_default=True,
        ),
    ]
