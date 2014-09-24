import itertools
import os
import os.path
import re
import requests
import zipfile

from celery.utils.log import get_task_logger
from django.conf import settings
from django.utils.translation import ugettext_lazy as _
from transitfeed import GetGtfsFactory, ProblemReporter, ProblemAccumulatorInterface
from transit_indicators.models import IndicatorJob
from urllib import urlencode

from datasources.models import Boundary, GTFSFeed, GTFSFeedProblem, OSMData, DemographicDataSource

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
    return re.sub("([a-z])([A-Z])", "\g<1> \g<2>",
                  problem.__class__.__name__)


def run_validate_gtfs(gtfsfeed_id):
    """Function to validate uploaded GTSFeed files

    Creates GTSFeedProblem objects for each error/warning
    and updates GTFSFeed processing status once completed.

    Arguments:
    :param gtfsfeed_id: ID of GTFSFeed object
    """

    gtfsfeed = GTFSFeed.objects.get(id=gtfsfeed_id)
    accumulator = OTIProblemAccumulator()
    problems = ProblemReporter(accumulator=accumulator)
    gtfs_factory = GetGtfsFactory()

    gtfsfeed.status = GTFSFeed.Statuses.VALIDATING
    gtfsfeed.save()

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

    # This adds a warning message to the import process if
    # both a shapes.txt and no shapes_dist_traveled are available
    # since length for routes and modes will not be available.
    # Note: In developing this, no gtfs files with a shape_dist_traveled,
    # but no shapes.txt file were able to be found.
    if len(schedule.GetShapeList()) == 0:
        # Check if shapes.txt of shape_dist_traveled exist
        stopdatetimes = itertools.chain(*[trip.GetStopTimes() for trip in schedule.trips.values()])
        if not any([stopdatetime.shape_dist_traveled for stopdatetime in stopdatetimes]):
            length_description = _('Unable to calculate route and system length without a shapes.txt' +
                                  ' or shapes_dist_traveled field in stop_times.txt')
            unused = GTFSFeedProblem.objects.create(gtfsfeed=gtfsfeed,
                                                    description=length_description,
                                                    title=_('Unable to calculate length'),
                                                    type=GTFSFeedProblem.ProblemTypes.WARNING)

    if errors_count == 0:
        gtfsfeed.status = GTFSFeed.Statuses.PROCESSING
        gtfsfeed.save()
    else:
        # Bail out if errors, no need to continue processing
        gtfsfeed.status = GTFSFeed.Statuses.ERROR
        gtfsfeed.save()
        return


    # delete any uploaded shapefiles that aren't for this GTFS' city
    delete_other_city_uploads(gtfsfeed.city_name)

    # invalidate last set of indicators calculated for this city (need to re-run them for this GTFS)
    IndicatorJob.objects.filter(city_name=gtfsfeed.city_name).update(is_latest_version=False)

    # send to GeoTrellis
    logger.debug('going to send gtfs to geotrellis')
    response = send_to_geotrellis(gtfsfeed.source_file)
    success = response.get('success', False)

    # Update processing status
    logger.debug('gtfs-parser status is %s', response)
    gtfsfeed.status = GTFSFeed.Statuses.COMPLETE if success else GTFSFeed.Statuses.ERROR
    if not success:
        title = _('GTFS Data load failed')
        msg = response.get('message', _('Unknown Error'))
        unused = GTFSFeedProblem.objects.create(gtfsfeed=gtfsfeed,
                                                description=msg,
                                                title=title,
                                                type=GTFSFeedProblem.ProblemTypes.ERROR)
    gtfsfeed.save()


def delete_other_city_uploads(cityname):
    """Helper function to delete uploaded shapefiles for cities other than the given city name.

    Arguments:
    :param cityname: String that is the name of the current city (keep files for this city)
    """
    logger.debug('going to delete uploads for cities other than %s', cityname)
    # deleting these data objects will cascade deletion of their related objects
    Boundary.objects.exclude(city_name=cityname).delete()
    OSMData.objects.exclude(city_name=cityname).delete()
    DemographicDataSource.objects.exclude(city_name=cityname).delete()

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
        if '/' in name: # the __MACOSX directory breaks the import
            continue    # but there shouldn't be any directories anyway.
        outfile = open(os.path.join(zip_dir, name), 'wb')
        outfile.write(zip_file.read(name))
        outfile.close()

    gtfs_response = {
        'success': False,
        'message': '',
    }

    try:
        params = {'gtfsDir': zip_dir}
        response = requests.post('http://localhost/gt/gtfs?%s' % urlencode(params))
        logger.debug('GeoTrellis response: %s', response.text)
        data = response.json()
        gtfs_response = data
    except ValueError as e:
        logger.exception('Error when parsing GeoTrellis response')
        gtfs_response['message'] = e.message

    return gtfs_response
