"""Handles downloading and importing OSM Data"""

import os
import subprocess
import tempfile

import requests
from celery.utils.log import get_task_logger

from django.conf import settings
from django.db import connection

from datasources.models import OSMData, OSMDataProblem
from datasources.tasks.shapefile import ErrorFactory

#  Note: The download is done using the overpass API
#  (see:http://wiki.openstreetmap.org/wiki/Overpass_API) because
#  we may be downloading large files and these endpoints are optimized
#  for downloads/reads unlike the main openstreetmap API endpoint
OSM_API_URL = 'http://www.overpass-api.de/api/xapi?way[bbox=%s,%s,%s,%s][highway=*]'

# set up shared task logger
logger = get_task_logger(__name__)


def run_osm_import(osmdata_id):
    """Download and run import step for OSM data

    Downloads and stores raw OSM data within a bounding box defined
    by imported GTFS data. Uses the SRID defined on the gtfs_stops
    table to determine correct UTM projection to import data as.

    Uses Raw SQL to
      - get extent from GTFS data since we
        do not have models that keeps track of GTFS Data
      - get UTM projection to import OSM data as correct projection
    """

    logger.debug('Starting OSM import')

    osm_data = OSMData.objects.get(pk=osmdata_id)
    osm_data.status = OSMData.Statuses.PROCESSING

    error_factory = ErrorFactory(OSMDataProblem, osm_data, 'osmdata')

    def handle_error(title, description):
        """Helper method to handle shapefile errors."""
        error_factory.error(title, description)
        osm_data.status = OSMData.Statuses.ERROR
        osm_data.save()
        return

    with connection.cursor() as c:

        try:
            # Get the bounding box for gtfs data
            # split components to make it easier to parse the sql response
            bbox_query = """
            SELECT MIN(ST_Xmin(the_geom)),
                   MIN(ST_Ymin(the_geom)),
                   MAX(ST_Xmax(the_geom)),
                   MAX(ST_Ymax(the_geom))
            FROM gtfs_stops;"""
            logger.debug('Making query for bounding box from gtfs stops')
            c.execute(bbox_query)
            bbox = c.fetchone()
        except Exception as e:
            err_msg = 'Error obtaining bounding box from gtfs_stops table'
            handle_error(err_msg, e.message)
        try:
            logger.debug('Making query for UTM projection srid from gtfs_stops table (geom field)')
            utm_projection_query = "SELECT FIND_SRID('', 'gtfs_stops', 'geom');"
            c.execute(utm_projection_query)
            utm_projection = c.fetchone()[0]
        except Exception as e:
            err_msg = 'Error obtaining SRID from gtfs_stops table'
            logger.exception(err_msg)
            handle_error(err_msg, e.message)


    _, temp_filename = tempfile.mkstemp()
    logger.debug('Generated tempfile %s to download osm data into', temp_filename)
    osm_data.source_file = temp_filename
    osm_data.status = OSMData.Statuses.DOWNLOADING
    osm_data.save()

    try:
        response = requests.get(OSM_API_URL % bbox, stream=True)

        logger.debug('Downloading OSM data from overpass/OSM api')
        # Download OSM data
        with open(temp_filename, 'wb') as fh:
            for chunk in response.iter_content(chunk_size=1024):
                if chunk:
                    fh.write(chunk)
                    fh.flush()
        logger.debug('Finished downloading OSM data')

        osm_data.status = OSMData.Statuses.IMPORTING
        osm_data.save()
    except Exception as e:
        err_msg = 'Error downloading data'
        logger.exception('Error downloading data')
        handle_error(err_msg, e.message)

    # Get Database settings
    db_host = settings.DATABASES['default']['HOST']
    db_password = settings.DATABASES['default']['PASSWORD']
    db_user = settings.DATABASES['default']['USER']
    db_name = settings.DATABASES['default']['NAME']
    env = os.environ.copy()
    env['PGPASSWORD'] = db_password

    # Insert OSM Data into Database with osm2pgsql command
    osm2pgsql_command = ['osm2pgsql',
                         '-U', db_user,
                         '-H', db_host,
                         '-d', db_name,
                         '-E', str(utm_projection),
                         temp_filename]
    try:
        logger.debug('Running OSM import command %s', ' '.join(osm2pgsql_command))
        subprocess.check_call(osm2pgsql_command, env=env)
        osm_data.status = OSMData.Statuses.COMPLETE
    except subprocess.CalledProcessError as e:
        osm_data.status = OSMData.Statuses.ERROR
        err_msg = 'Error running osm2pgsql command'
        logger.exception('Error running osm2pgsql command')
        error_factory.error(err_msg, e.message)
    finally:
        osm_data.save()
        os.remove(temp_filename)
