# encoding: utf8
from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('datasources', '0001_initial'),
    ]

    operations = [
        migrations.AddField(
            model_name='gtfsfeed',
            name='validation_results_file',
            field=models.FileField(null=True, upload_to='', blank=True),
            preserve_default=True,
        ),
        migrations.AddField(
            model_name='gtfsfeed',
            name='is_valid',
            field=models.NullBooleanField(),
            preserve_default=True,
        ),
    ]
