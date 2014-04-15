from transitfeed import (
    GetGtfsFactory,
    ProblemReporter,
    ProblemAccumulatorInterface)
import re
from datasources.models import GTFSFeed, GTFSFeedProblem

from transit_indicators.celery_settings import app
from celery.utils.log import get_task_logger

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
    gtfsfeedproblems = []
    for problem in accumulator.problems:
        type = (GTFSFeedProblem.ProblemTypes.WARNING
                if problem.IsWarning()
                else GTFSFeedProblem.ProblemTypes.ERROR)
        description = problem.FormatProblem()
        title = get_problem_title(problem)
        gtfsfeedproblem = GTFSFeedProblem(
            gtfsfeed=gtfsfeed,
            description=description,
            title=title,
            type=type)
        gtfsfeedproblems.append(gtfsfeedproblem)
    logger.debug('Found %s problems in %s gtfs file',
                 len(gtfsfeedproblems),
                 gtfsfeed.source_file)
    GTFSFeedProblem.objects.bulk_create(gtfsfeedproblems)

    # Update processing status
    gtfsfeed.is_processed = True
    gtfsfeed.save()
