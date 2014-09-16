"""Handles sending realtime data to geotrellis for processing"""

import os
import subprocess
import tempfile

import requests
from celery.utils.log import get_task_logger

from django.conf import settings
from django.db import connection

from datasources.models import RealTime, RealTimeProblem
from datasources.tasks.shapefile import ErrorFactory

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

    result = send_to_geotrellis(real_time.source_file)
    real_time.is_processed = result
    real_time.save()

def send_to_geotrellis(realtime_file):
    """ Send a real time file to geotrellis for import """

    try:
        params = { 'file': realtime_file.path }
        response = requests.post('http://localhost/gt/real-time/import', params=params)
        logger.debug('Requested url: %s', response.url)
        logger.debug('Geotrellis response RealTime: %s', response.text)
        # TODO: Add geotrellis handling of real-time data
        #data = response.json()
        success = True
    except ValueError as e:
        logger.exception('Error parsing geotrellis RealTime response: %s', e.message)
        success = False
    return success


