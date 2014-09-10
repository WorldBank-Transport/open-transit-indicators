import json
from time import sleep

from celery.utils.log import get_task_logger
import requests
from transit_indicators.models import IndicatorJob
from userdata.models import OTIUser

logger = get_task_logger(__name__)
GT_INDICATORS_ENDPOINT = 'http://localhost/gt/indicators'

def run_indicator_calculation(indicator_job):
    indicator_job.job_status = IndicatorJob.StatusChoices.PROCESSING
    indicator_job.save()

    headers = {'Content-type': 'application/json', 'Accept': 'application/json'}
    token = OTIUser.objects.get(username='oti-admin').auth_token.key
    payload = json.dumps({
            'token': token,
            'version': indicator_job.version,
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
    response = requests.post(GT_INDICATORS_ENDPOINT, data=payload, headers=headers)

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
        IndicatorJob.objects.update(is_latest_version=False)
        indicator_job.is_latest_version = True
        indicator_job.save()
        
