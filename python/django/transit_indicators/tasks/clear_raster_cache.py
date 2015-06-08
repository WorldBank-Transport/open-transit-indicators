import os

from celery.utils.log import get_task_logger

LOGGER = get_task_logger(__name__)

CACHE_DIR = '/projects/open-transit-indicators/scala/opentransit/data/data'
CACHE_KEY_PREFIXES = ['jobs_travelshed', 'jobs_absolute_travelshed', 'jobs_percentage_travelshed']


def clear_raster_cache(indicator_job_id):
    """Deletes files from the jobs indicator raster cache."""
    id_str = str(indicator_job_id)
    LOGGER.info('Deleting cached raster files for indicator job #%s.', indicator_job_id)
    try:
        for cache_key_prefix in CACHE_KEY_PREFIXES:
            arg_file = '%s%s%s' % (cache_key_prefix, id_str, '.arg')
            json_file = '%s%s%s' % (cache_key_prefix, id_str, '.json')
            os.remove(os.path.join(CACHE_DIR, arg_file))
            os.remove(os.path.join(CACHE_DIR, json_file))
    except OSError as ex:
        LOGGER.error('Error deleting cached raster files for indicator job #%s:', indicator_job_id)
        LOGGER.error(str(ex))
