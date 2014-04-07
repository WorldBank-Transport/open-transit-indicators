from __future__ import absolute_import
from os import path
from subprocess import Popen

from celery.utils.log import get_task_logger
from django.conf import settings
from django.core.files import File

from .celery import app
from datasources.models import GTFSFeed

# set up shared task logger
logger = get_task_logger(__name__)


@app.task
def verify_gtfs(feed_id):
    logger.info('in verify_gtfs...')
    logger.info('media root: %s', settings.MEDIA_ROOT)

    feed = GTFSFeed.objects.get(pk=feed_id)
    source_file_name = feed.source_file.name

    gtfs_path = path.join(settings.MEDIA_ROOT, path.normpath(source_file_name))
    logger.info('Verifying GTFS %s', gtfs_path)
    output_file_name = path.splitext(path.basename(source_file_name))[0] + '.html'
    logger.info('Output file name: %s', output_file_name)
    output_path = path.join(settings.MEDIA_ROOT, output_file_name)
    logger.info('Writing validation output to %s', output_path)

    if not path.isfile(gtfs_path):
        logger.error('Input file does not exist at path %s!', gtfs_path)
        return False

    logger.info('Got input file.  Validating...')
    p = Popen(['feedvalidator.py', '-m', '-n', '-o', output_path, gtfs_path])
    stdout, stderr = p.communicate()
    if stdout:
        logger.debug(stdout)

    if stderr:
        logger.error(stderr)

    inspect_validation_output(output_path, feed)
    logger.info('Finished in verify_gtfs task.')
    return True


def inspect_validation_output(validation_file, feed):
    """Helper function to look at output from feedvalidator.py, and update GTFSFeed object.

    Arguments:
    validation_file:  Full path to html file output from feedvalidator.py
    feed:  GTFSFeed model instance (will update is_valid and validation file path)
    """
    if not path.isfile(validation_file):
        logger.error('File %s does not exist in inspect_validation_output.', validation_file)
        return False

    with open(validation_file, 'rb') as f:
        # add Django file object to model instance
        feed.validation_results_file.save(validation_file, File(f), save=False)

    # TODO: Parse validation output to determine if there were feed errors or not,
    # then set feed.is_valid accordingly.
    feed.is_valid = True
    feed.save()
    return True
