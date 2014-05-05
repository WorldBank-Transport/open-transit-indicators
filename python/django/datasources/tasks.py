import os
import os.path
import re
import requests
import zipfile

from celery.utils.log import get_task_logger
from django.conf import settings
from transitfeed import GetGtfsFactory, ProblemReporter, ProblemAccumulatorInterface
from urllib import urlencode

from datasources.models import GTFSFeed, GTFSFeedProblem
from transit_indicators.celery_settings import app

# set up shared task logger
logger = get_task_logger(__name__)


class OTIProblemAccumulator(ProblemAccumulatorInterface):
    """Tracks problems of GTFS files

    Differs from existing accumulators primarily by not
    printing to file and instead saving problems in a local
    list that is used later to save individual problems in
    the database.
    """

    def __init__(self):
        self.problems = []

    def _Report(self, e):
        self.problems.append(e)


def get_problem_title(problem):
    """Helper function to transform camelCase to Title Case"""
    return re.sub("([a-z])([A-Z])","\g<1> \g<2>",
                  problem.__class__.__name__)


@app.task
def validate_gtfs(gtfsfeed_id):
    """Function to validate uploaded GTSFeed files

    Creates GTSFeedProblem objects for each error/warning
    and updates GTFSFeed processing status once completed.

    is_valid == null indicates job not started
    Valid gtfsfeed files have state is_valid = True && is_processed = True
    is_valid = True && is_processed = False indicate a geotrellis error

    Arguments:
    :param gtfsfeed_id: ID of GTFSFeed object
    """
    gtfsfeed = GTFSFeed.objects.get(id=gtfsfeed_id)
    accumulator = OTIProblemAccumulator()
    problems = ProblemReporter(accumulator=accumulator)
    gtfs_factory = GetGtfsFactory()

    # load gtfs file(s)
    loader = gtfs_factory.Loader(
        gtfsfeed.source_file,
        problems=problems,
        extra_validation=False,
        memory_db=True,
        check_duplicate_trips=True,
        gtfs_factory=gtfs_factory)

    # loads GTFS file and performs validation
    logger.debug('Loading %s gtfs file for validation', gtfsfeed.source_file)
    schedule = loader.Load()
    logger.debug('Finished loading gtfs file')

    # save individual problems in database
    errors_count = 0
    warnings_count = 0
    for problem in accumulator.problems:
        type = (GTFSFeedProblem.ProblemTypes.WARNING
                if problem.IsWarning()
                else GTFSFeedProblem.ProblemTypes.ERROR)
        description = problem.FormatProblem()
        title = get_problem_title(problem)
        obj, created = GTFSFeedProblem.objects.get_or_create(
            gtfsfeed=gtfsfeed,
            description=description,
            title=title,
            type=type)
        if created:
            if type == GTFSFeedProblem.ProblemTypes.ERROR:
                errors_count += 1
            elif type == GTFSFeedProblem.ProblemTypes.WARNING:
                warnings_count += 1

    logger.debug('Found %s problems in %s gtfs file',
                 errors_count + warnings_count,
                 gtfsfeed.source_file)

    gtfsfeed.is_valid = True if errors_count == 0 else False
    # send to GeoTrellis
    result = send_to_geotrellis(gtfsfeed.source_file) if gtfsfeed.is_valid else False

    # Update processing status
    gtfsfeed.is_processed = result
    gtfsfeed.save()


def send_to_geotrellis(gtfs_file):
    """Sends GTFS data to GeoTrellis for storage

    Note: this makes use of the current assumption that GeoTrellis is running
    on the same machine as Django, and can therefore share files. If that ever
    changes, this will need to be altered to either send the GTFS zip file itself
    (and add unzipping to the GeoTrellis portion), or use a shared resource,
    such as s3, and add URI-fetching to the GeoTrellis portion.

    Arguments:
    :returns: Whether or not the file was processed successfully
    """
    zip_dir = '%s_unzipped' % gtfs_file.path
    if not os.path.exists(zip_dir):
        os.makedirs(zip_dir)

    zip_file = zipfile.ZipFile(gtfs_file)
    for name in zip_file.namelist():
        outfile = open(os.path.join(zip_dir, name), 'wb')
        outfile.write(zip_file.read(name))
        outfile.close()

    try:
        params = {'gtfsDir': zip_dir}
        response = requests.post('http://localhost/gt/gtfs?%s' % urlencode(params))
        logger.debug('GeoTrellis response: %s', response.text)
        data = response.json()
        success = data['success']
    except ValueError:
        logger.exception('Error when parsing GeoTrellis response')
        success = False

    return success
