# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations

from transit_indicators.gtfs import GTFSRouteTypes


def forwards(apps, schema_editor):
    """ Create a table row for each entry in GTFSRouteType.CHOICES

    Note that this migration will use whatever the current CHOICES are in the GTFSRouteTypes models,
    and not the CHOICES that were defined at the time the migration was created.

    TODO: Create a management command to update the GTFSRouteTypes (and call it here?)

    """
    GTFSRouteType = apps.get_model('transit_indicators', 'GTFSRouteType')
    for choice in GTFSRouteTypes.CHOICES:
        gtfs_route_type = GTFSRouteType.objects.update_or_create(route_type=choice[0], description=choice[1])


def backwards(apps, schema_editor):
    """ Destroy all route type rows before deleting table"""
    GTFSRouteType = apps.get_model('transit_indicators', 'GTFSRouteType')
    gtfs_route_types = GTFSRouteType.objects.all()
    gtfs_route_types.delete()


class Migration(migrations.Migration):

    dependencies = [
        ('transit_indicators', '0022_gtfsroutetype'),
    ]

    operations = [
        migrations.RunPython(forwards, backwards),
    ]
