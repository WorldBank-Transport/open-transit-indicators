from os import path
import subprocess

from celery.utils.log import get_task_logger
from django.conf import settings
from django.core.files import File

from celery_settings import app
from datasources.models import GTFSFeed

# set up shared task logger
logger = get_task_logger(__name__)


@app.task
def verify_gtfs(feed_id):
    """Celery task to take an uploaded GTFS file, run feed validation on it, then save results"""
    
    logger.debug('Starting verify_gtfs task.')
    feed = GTFSFeed.objects.get(pk=feed_id)
    source_file_name = feed.source_file.name
    gtfs_path = path.join(settings.MEDIA_ROOT, path.normpath(source_file_name))
    logger.info('Verifying GTFS %s', gtfs_path)
    output_file_name = path.splitext(path.basename(source_file_name))[0] + '.txt'
    logger.info('Output file name: %s', output_file_name)
    output_path = path.join(settings.MEDIA_ROOT, output_file_name)
    logger.info('Writing validation output to %s', output_path)

    if not path.isfile(gtfs_path):
        logger.error('Input file does not exist at path %s!', gtfs_path)
        return False

    logger.debug('Got input file.  Validating...')
    p = subprocess.Popen(['feedvalidator.py', '-m', '-n', '-o', 'CONSOLE', gtfs_path],
                         stdout=subprocess.PIPE)
    stdout, stderr = p.communicate()
    
    if stdout:
        logger.debug(stdout)
        result = stdout.split('\n')
        errct = result[-2:-1][0] # output line with count of errors/warnings
        if errct.find('error') > -1:
            logger.info('found error(s): %s', errct)
            feed.is_valid = False
        else:
            feed.is_valid = True
            if errct.find('successfully') > -1:
                logger.info('no errors or warnings found.')
            else:
                # have warnings
                logger.info('found warnings: ' + errct[7:])
                
        feed.validation_summary = errct
        
        with open(output_path, 'wb') as f:
            f.write(stdout)

        with open(output_path, 'rb') as f:
            feed.validation_results_file.save(output_path, File(f), save=False)
            
        feed.save()
    else:
        logger.error('no output returned from validation run!')
        return False

    if stderr:
        logger.error(stderr)
        return False

    
    # inspect_validation_output(output_path, feed)
    logger.debug('Finished in verify_gtfs task.')
    return True
