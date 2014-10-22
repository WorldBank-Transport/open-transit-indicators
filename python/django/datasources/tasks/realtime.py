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
    """ Run the RealStopTime import job """
    logger.debug("Starting RealTime import: %d", realtime_id)

    real_time = RealTime.objects.get(pk=realtime_id)
    real_time.status = RealTime.Statuses.IMPORTING
    real_time.save()
    error_factory = ErrorFactory(RealTimeProblem, real_time, 'realtime')

    def handle_error(title, description):
        error_factory.error(title, description)
        real_time.status = RealTime.Statuses.ERROR
        real_time.save()
        return

    result = load_stop_times(real_time, error_factory)

    if result['imported'] == 0:
        msg = '0 of %i rows', result['total']
        handle_error('No Rows Imported', msg)
    else:
        real_time.status = RealTime.Statuses.COMPLETE
        real_time.save()

def load_stop_times(real_time, error_factory):
    """ Load stop times into RealStopTime model

    The file to import should match the GTFS spec for stop_times.txt

    """

    BATCH_SIZE = 5000

    def ensure_row_key(row, key, default=None):
        val = row.get(key, default)
        if not val:
            val = default
        row[key] = val

    def insert_realstoptimes(realstoptimes, line):
        try:
            RealStopTime.objects.bulk_create(realstoptimes)
        except Exception:
            for num, realstoptime in enumerate(realstoptimes, line):
                try:
                    realstoptime.save()
                except Exception as e:
                    logger.debug('Row %d error: %s', num, e)
                    key = 'Row %i' % num
                    error_factory.warn(key, e.message)

    imported = 0
    try:
        with open(real_time.source_file.path, 'r') as stop_times_file:
            dict_reader = csv.DictReader(stop_times_file)
            stop_time_objects = []
            for line, row in enumerate(dict_reader, start=1):
                # Ensure optionals have proper defaults
                ensure_row_key(row, 'pickup_type', 0)
                ensure_row_key(row, 'drop_off_type', 0)
                ensure_row_key(row, 'shape_dist_traveled')

                stop_time_object = RealStopTime(datasource=real_time, **row)
                stop_time_objects.append(stop_time_object)
                imported += 1

                if line % BATCH_SIZE == 0:
                    # line - BATCH_SIZE is passed to insert_realstoptimes
                    # so it knows what line this batch begins on
                    insert_realstoptimes(stop_time_objects, line - BATCH_SIZE)
                    stop_time_objects = []
                    logger.debug('Imported objects %i of %i', imported, line)

            insert_realstoptimes(stop_time_objects, line)
    except Exception as e:
        error_factory.error('Import failed', e.message)
        imported = 0

    if imported > 0:
        # Delete all other stop times not of this import
        logger.debug('Cleaning old stop time entries...')
        other_stop_times = RealStopTime.objects.exclude(datasource=real_time)
        other_stop_times.delete()

    return {
        'imported': imported,
        'total': line
    }
