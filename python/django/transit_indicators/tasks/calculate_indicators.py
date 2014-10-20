import json
from time import sleep

from celery.utils.log import get_task_logger
from django.conf import settings
import requests

from datasources.models import DemographicDataSource, DemographicDataFeature, RealTime, OSMData
from transit_indicators.models import IndicatorJob, OTIIndicatorsConfig
from userdata.models import OTIUser

logger = get_task_logger(__name__)

def run_realtime_indicators():
    """Helper function that returns True if indicators which depend upon realtime
    data can be run"""
    realtime_datasource = RealTime.objects.filter().first()
    return realtime_datasource.is_complete()

def run_osm_indicators():
    """Helper function that returns True if indicators which depend upon osm
    data can be run"""
    osm_datasource = OSMData.objects.filter().first()
    return osm_datasource.is_complete()

def run_demographics_indicators():
    """Helper function that returns True if accessibility calculators can be run"""
    demographic_datasource = DemographicDataSource.objects.filter().first()
    has_features = DemographicDataFeature.objects.filter().count() > 0
    return demographic_datasource.is_complete() and has_features

def run_indicator_calculation(indicator_job):
    """Initiate celery job which tells scala to calculate indicators"""
    logger.debug('Starting indicator job: %s for city %s', indicator_job, indicator_job.city_name)
    indicator_job.job_status = IndicatorJob.StatusChoices.PROCESSING
    indicator_job.save()

    headers = {'Content-type': 'application/json', 'Accept': 'application/json'}

    config = OTIIndicatorsConfig.objects.get()
    token = OTIUser.objects.get(username='oti-admin').auth_token.key
    payload = json.dumps({
        'token': token,
        'version': indicator_job.version,
        'avg_fare': config.avg_fare,
        'nearby_buffer_distance_m': config.nearby_buffer_distance_m,
        'poverty_line': config.poverty_line,
        'max_commute_time_s': config.max_commute_time_s,
        'max_walk_time_s': config.max_walk_time_s,
        'city_boundary_id': config.city_boundary.id if config.city_boundary else 0,
        'region_boundary_id': config.region_boundary.id if config.region_boundary else 0,
        'params_requirements': {
            'demographics': run_demographics_indicators(),
            'osm': run_osm_indicators(),
            'observed': run_realtime_indicators(),
            'city_bounds': True if config.city_boundary else False,
            'region_bounds': True if config.region_boundary else False
        },
        'sample_periods': [
            {
                'id': s.id,
                'type': s.type,
                'period_start': s.period_start.isoformat(),
                'period_end': s.period_end.isoformat(),
            }
            for s in indicator_job.sample_periods.all()
        ]
    })

    logger.debug('Payload JSON: %s ', payload)
    response = requests.post(settings.SCALA_ENDPOINTS['INDICATORS'], data=payload, headers=headers)

    if response.status_code != 201:
        logger.error('%d encountered', response.status_code)
        logger.error(response.text)
        indicator_job.job_status = IndicatorJob.StatusChoices.ERROR
        indicator_job.save()
        response.raise_for_status()
    else:
        logger.info('Response: %s ', response.json())
        logger.info('Calculations triggered for version: %s', indicator_job.version)

    # re-query to get object status
    indicator_job = IndicatorJob.objects.get(pk=indicator_job.pk)
    status = indicator_job.job_status

    # Wait for job status to change from processing before finishing job
    while status == IndicatorJob.StatusChoices.PROCESSING:
        sleep(10)
        # re-query to get object status
        indicator_job = IndicatorJob.objects.get(pk=indicator_job.pk)
        status = indicator_job.job_status
        logger.info('Checking job status for indicator job version: %s', indicator_job.version)

    # Update current indicator version on successful completion
    if status == IndicatorJob.StatusChoices.COMPLETE:
        logger.info('Job completed successfully; updating current indicator version to %s',
                    indicator_job.version)
        # invalidate previous indicator calculations for city
        IndicatorJob.objects.filter(city_name=indicator_job.city_name).update(is_latest_version=False)
        indicator_job.is_latest_version = True
        indicator_job.save()
    else:
        logger.error('Indicator calculation job failed with status: %s', status)
