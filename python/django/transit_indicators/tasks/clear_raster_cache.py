import os

from celery.utils.log import get_task_logger

logger = get_task_logger(__name__)

CACHE_DIR = '/projects/open-transit-indicators/scala/opentransit/data/data'
CACHE_KEY_PREFIX = 'jobs_travelshed'


def clear_raster_cache(indicator_job_id):
    """Deletes files from the jobs indicator raster cache."""
    id_str = str(indicator_job_id)
    logger.info('Deleting cached raster files for indicator job #%s.', indicator_job_id)
    try:
        arg_file = '%s%s%s' % (CACHE_KEY_PREFIX, id_str, '.arg')
        json_file = '%s%s%s' % (CACHE_KEY_PREFIX, id_str, '.json')
        os.remove(os.path.join(CACHE_DIR, arg_file))
        os.remove(os.path.join(CACHE_DIR, json_file))
    except OSError as e:
        logger.error('Error deleting cached raster files for indicator job #%s:', indicator_job_id)
        logger.error(str(e))
