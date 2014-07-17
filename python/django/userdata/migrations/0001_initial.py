# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations
import django.utils.timezone
import django.core.validators


class Migration(migrations.Migration):

    dependencies = [
    ]

    operations = [
        migrations.CreateModel(
            name='OTIUser',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('password', models.CharField(max_length=128, verbose_name='password')),
                ('last_login', models.DateTimeField(default=django.utils.timezone.now, verbose_name='last login')),
                ('username', models.CharField(help_text=b'Required. 30 characters or fewer. Letters, digits and @/./+/-/_ only.', unique=True, max_length=30, verbose_name=b'username', validators=[django.core.validators.RegexValidator(b'^[\\w.@+-]+$', b'Enter a valid username.', b'invalid')])),
                ('email', models.EmailField(max_length=254, verbose_name=b'email', blank=True)),
                ('first_name', models.CharField(max_length=30, verbose_name=b'first_name', blank=True)),
                ('last_name', models.CharField(max_length=30, verbose_name=b'last_name', blank=True)),
                ('is_staff', models.BooleanField(default=False, verbose_name=b'is_staff')),
                ('is_active', models.BooleanField(default=True, verbose_name=b'is_active')),
                ('is_superuser', models.BooleanField(default=False, verbose_name=b'is_superuser')),
                ('date_joined', models.DateTimeField(auto_now_add=True, verbose_name=b'date_joined')),
            ],
            options={
                'abstract': False,
            },
            bases=(models.Model,),
        ),
    ]
