
from time import sleep

from celery.utils.log import get_task_logger
import requests
from transit_indicators.models import IndicatorJob

logger = get_task_logger(__name__)
GT_INDICATORS_ENDPOINT = 'http://localhost/gt/indicators'

def run_indicator_calculation(indicator_job_id):
    indicator_job = IndicatorJob.objects.get(pk=indicator_job_id)

    indicator_job.job_status = IndicatorJob.StatusChoices.PROCESSING
    indicator_job.save()

    logger.debug('Payload JSON: %s ', indicator_job)
    headers = {'Content-type': 'application/json', 'Accept': 'application/json'}
    response = requests.post(GT_INDICATORS_ENDPOINT, data=indicator_job.payload, headers=headers)

    if response.status_code != 201:
        logger.error('%d encountered', response.status_code)
        logger.error(response.text)
        indicator_job.job_status = IndicatorJob.StatusChoices.ERROR
        indicator_job.save()
        response.raise_for_status()
    else:
        logger.info('Response: %s ', response.json())
        logger.info('Calculations triggered for version: %i', indicator_job.version)

    status = indicator_job.job_status

    # Wait for job status to change from processing before finishing job
    while status == IndicatorJob.StatusChoices.PROCESSING:
        sleep(10)
        status = IndicatorJob.objects.get(pk=indicator_job_id).job_status
        logger.info('Checking job status for indicator job version: %s', indicator_job.version)
