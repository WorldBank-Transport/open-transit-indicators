"""Handles sending realtime data to geotrellis for processing"""
import csv
import os
import subprocess
import tempfile

import requests
from celery.utils.log import get_task_logger

from django.conf import settings
from django.db import connection, transaction

from datasources.models import RealTime, RealTimeProblem
from datasources.tasks.shapefile import ErrorFactory
from gtfs_realtime.models import RealStopTime

# set up shared task logger
logger = get_task_logger(__name__)


def run_realtime_import(realtime_id):
    """ Wrapper job to asynchronously post the realtime data to geotrellis """
    logger.debug("Starting RealTime import")

    real_time = RealTime.objects.get(pk=realtime_id)
    error_factory = ErrorFactory(RealTimeProblem, real_time, 'realtime')

    def handle_error(title, description):
        error_factory.error(title, description)
        real_time.is_processed = False
        real_time.save()
        return

    result = load_stop_times(real_time, error_factory)

    if result['imported'] == 0:
        msg = '0 of %i rows', result['total']
        handle_error('No Rows Imported', msg)
    else:
        real_time.is_processed = True
        real_time.save()

def load_stop_times(real_time, error_factory):
    """ Load stop times into RealStopTime model """

    BATCH_SIZE = 1000

    def ensure_row_key(row, key, default=None):
        val = row.get(key, default)
        if not val:
            val = default
        row[key] = val

    line = 0
    imported = 0
    with open(real_time.source_file.path, 'r') as stop_times_file:
        dict_reader = csv.DictReader(stop_times_file)
        stop_time_objects = []
        for row in dict_reader:
            try:
                # Ensure optionals have proper defaults
                ensure_row_key(row, 'pickup_type', 0)
                ensure_row_key(row, 'drop_off_type', 0)
                ensure_row_key(row, 'shape_dist_traveled')

                real_stop_time = RealStopTime(datasource=real_time, **row)
                # TODO: Speed up with bulk_create?
                #       5 mb file takes ~4 min
                #       Would require more strict error checking of params because
                #       a single insert failure in a bulk_create cancels all inserts
                real_stop_time.save()
                imported += 1
            except Exception as e:
                print e.message
                key = 'Row %i' % line
                error_factory.warn(key, e.message)

            line += 1

            if line % BATCH_SIZE == 0:
                print 'Imported objects %i of %i' % (imported, line)

    if imported > 0:
        # Delete all other stop times not of this import
        print 'Cleaning old stop time entries...'
        other_stop_times = RealStopTime.objects.exclude(datasource=real_time)
        other_stop_times.delete()

    return {
        'imported': imported,
        'total': line
    }
