# encoding: utf8
from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('datasources', '0002_auto_20140408_1422'),
    ]

    operations = [
        migrations.AddField(
            model_name='gtfsfeed',
            name='validation_summary',
            field=models.CharField(default='', max_length=255, blank=True),
            preserve_default=True,
        ),
    ]
