# encoding: utf8
from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('datasources', '0003_gtfsfeed_validation_summary'),
    ]

    operations = [
        migrations.CreateModel(
            name='GTFSFeedProblem',
            fields=[
                (u'id', models.AutoField(verbose_name=u'ID', serialize=False, auto_created=True, primary_key=True)),
                ('type', models.CharField(max_length=3, choices=[('err', 'Error'), ('war', 'Warning')])),
                ('description', models.TextField()),
                ('title', models.CharField(max_length=255)),
                ('gtfsfeed', models.ForeignKey(to='datasources.GTFSFeed', to_field=u'id')),
            ],
            options={
            },
            bases=(models.Model,),
        ),
        migrations.AddField(
            model_name='gtfsfeed',
            name='is_processed',
            field=models.BooleanField(default=False),
            preserve_default=True,
        ),
        migrations.RemoveField(
            model_name='gtfsfeed',
            name='validation_results_file',
        ),
        migrations.RemoveField(
            model_name='gtfsfeed',
            name='validation_summary',
        ),
    ]
