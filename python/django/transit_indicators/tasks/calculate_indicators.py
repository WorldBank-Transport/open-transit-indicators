import json
from time import sleep

from celery.utils.log import get_task_logger
from django.conf import settings
from rest_framework import status as status_codes
import requests

from datasources.models import DemographicDataSource, DemographicDataFeature, RealTime, OSMData
from transit_indicators.models import IndicatorJob, OTIIndicatorsConfig, SamplePeriod
from userdata.models import OTIUser

logger = get_task_logger(__name__)


def is_complete(file_data_source_instance):
    """Abstraction to check on completion of file data sources"""
    if file_data_source_instance:
        return file_data_source_instance.status == file_data_source_instance.Statuses.COMPLETE
    else:
        return False


def run_realtime_indicators():
    """Helper function that returns True if indicators which depend upon realtime
    data can be run"""
    realtime_datasource = RealTime.objects.filter().first()
    return is_complete(realtime_datasource)


def run_osm_indicators():
    """Helper function that returns True if indicators which depend upon osm
    data can be run"""
    osm_datasource = OSMData.objects.filter().first()
    return is_complete(osm_datasource)


def run_demographics_indicators():
    """Helper function that returns True if accessibility calculators can be run"""
    demographic_datasource = DemographicDataSource.objects.filter().first()
    has_features = DemographicDataFeature.objects.filter().count() > 0
    return is_complete(demographic_datasource) and has_features

def run_job_indicators():
    """ Helper funciton that returns True if jobs indicators can be run """
    # TODO: Implement this for real.
    True


def run_indicator_calculation(indicator_job):
    """Initiate celery job which tells scala to calculate indicators"""
    logger.debug('Starting indicator job: %s for city %s', indicator_job, indicator_job.city_name)
    indicator_job.job_status = IndicatorJob.StatusChoices.PROCESSING
    indicator_job.save()

    # Set db names and sample periods based on whether or not this is a scenario calculation.
    # There are two databases references:
    #   gtfs - Holds all GTFS data
    #   aux  - Holds all auxiliary data - OSM/demographics/boundaries/etc.
    # For non-scenarios, these both refer to the same database.
    # For scenarios, the aux db refers to the one that was initialized during scenario the creation.
    default_db_name = settings.DATABASES['default']['NAME']
    scenario = indicator_job.scenario
    if scenario:
        sample_periods = [scenario.sample_period]
        gtfs_db_name = scenario.db_name
        aux_db_name = default_db_name
    else:
        # No longer exclude the alltime period here.
        # It is now use by the scala code in order to signal that alltime needs to be calculated.
        sample_periods = SamplePeriod.objects.all()
        gtfs_db_name = default_db_name
        aux_db_name = default_db_name

    headers = {'Content-type': 'application/json', 'Accept': 'application/json'}

    config = OTIIndicatorsConfig.objects.get()
    token = OTIUser.objects.get(username='oti-admin').auth_token.key
    payload = json.dumps({
        'token': token,
        'id': indicator_job.id,
        'avg_fare': config.avg_fare,
        'nearby_buffer_distance_m': config.nearby_buffer_distance_m,
        'poverty_line': config.poverty_line,
        'arrive_by_time_s': config.arrive_by_time_s,
        'max_commute_time_s': config.max_commute_time_s,
        'max_walk_time_s': config.max_walk_time_s,
        'city_boundary_id': config.city_boundary.id if config.city_boundary else 0,
        'region_boundary_id': config.region_boundary.id if config.region_boundary else 0,
        'gtfs_db_name': gtfs_db_name,
        'aux_db_name': aux_db_name,
        'params_requirements': {
            'demographics': run_demographics_indicators(),
            'osm': run_osm_indicators(),
            'observed': run_realtime_indicators(),
            'city_bounds': bool(config.city_boundary),
            'region_bounds': bool(config.region_boundary),
            'job_demographics': run_job_indicators()
        },
        'sample_periods': [
            {
                'id': s.id,
                'type': s.type,
                'period_start': s.period_start.isoformat(),
                'period_end': s.period_end.isoformat(),
            }
            for s in sample_periods
        ]
    })

    logger.debug('Payload JSON: %s ', payload)
    response = requests.post(settings.SCALA_ENDPOINTS['INDICATORS'], data=payload, headers=headers)

    if response.status_code != status_codes.HTTP_202_ACCEPTED:
        logger.error('%d encountered', response.status_code)
        logger.error(response.text)
        indicator_job.job_status = IndicatorJob.StatusChoices.ERROR
        indicator_job.save()
        response.raise_for_status()
    else:
        logger.info('Response: %s ', response.json())
        logger.info('Calculation job %s triggered', indicator_job.id)

    # re-query to get object status
    indicator_job = IndicatorJob.objects.get(pk=indicator_job.pk)
    status = indicator_job.job_status

    # Wait for job status to change from processing before finishing job
    while status == IndicatorJob.StatusChoices.PROCESSING:
        sleep(10)
        # re-query to get object status
        indicator_job = IndicatorJob.objects.get(pk=indicator_job.pk)
        status = indicator_job.job_status
        logger.info('Checking job status for indicator job %s', indicator_job.id)

    # Update current indicator version on successful completion
    if status == IndicatorJob.StatusChoices.COMPLETE:
        logger.info('Indicator job %s completed successfully.',
                    indicator_job.id)
        # invalidate previous indicator calculations for city
        indicator_job.save()
    else:
        logger.error('Indicator calculation job failed with status: %s', status)
