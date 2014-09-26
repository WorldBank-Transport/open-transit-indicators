# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('datasources', '0014_remove_demographicdatasource_is_loaded'),
    ]

    operations = [
        migrations.AlterField(
            model_name='boundary',
            name='status',
            field=models.CharField(default=b'pending', max_length=16, choices=[(b'pending', 'Pending'), (b'uploading', 'Uploading'), (b'uploaded', 'Uploaded'), (b'processing', 'Processing'), (b'downloading', 'Downloading'), (b'importing', 'Importing'), (b'waiting_input', 'Waiting for User Input'), (b'validating', 'Validating'), (b'complete', 'Complete'), (b'warning', 'Warning'), (b'error', 'Error')]),
        ),
        migrations.AlterField(
            model_name='demographicdatasource',
            name='status',
            field=models.CharField(default=b'pending', max_length=16, choices=[(b'pending', 'Pending'), (b'uploading', 'Uploading'), (b'uploaded', 'Uploaded'), (b'processing', 'Processing'), (b'downloading', 'Downloading'), (b'importing', 'Importing'), (b'waiting_input', 'Waiting for User Input'), (b'validating', 'Validating'), (b'complete', 'Complete'), (b'warning', 'Warning'), (b'error', 'Error')]),
        ),
        migrations.AlterField(
            model_name='gtfsfeed',
            name='status',
            field=models.CharField(default=b'pending', max_length=16, choices=[(b'pending', 'Pending'), (b'uploading', 'Uploading'), (b'uploaded', 'Uploaded'), (b'processing', 'Processing'), (b'downloading', 'Downloading'), (b'importing', 'Importing'), (b'waiting_input', 'Waiting for User Input'), (b'validating', 'Validating'), (b'complete', 'Complete'), (b'warning', 'Warning'), (b'error', 'Error')]),
        ),
        migrations.AlterField(
            model_name='osmdata',
            name='status',
            field=models.CharField(default=b'pending', max_length=16, choices=[(b'pending', 'Pending'), (b'uploading', 'Uploading'), (b'uploaded', 'Uploaded'), (b'processing', 'Processing'), (b'downloading', 'Downloading'), (b'importing', 'Importing'), (b'waiting_input', 'Waiting for User Input'), (b'validating', 'Validating'), (b'complete', 'Complete'), (b'warning', 'Warning'), (b'error', 'Error')]),
        ),
        migrations.AlterField(
            model_name='realtime',
            name='status',
            field=models.CharField(default=b'pending', max_length=16, choices=[(b'pending', 'Pending'), (b'uploading', 'Uploading'), (b'uploaded', 'Uploaded'), (b'processing', 'Processing'), (b'downloading', 'Downloading'), (b'importing', 'Importing'), (b'waiting_input', 'Waiting for User Input'), (b'validating', 'Validating'), (b'complete', 'Complete'), (b'warning', 'Warning'), (b'error', 'Error')]),
        ),
    ]
