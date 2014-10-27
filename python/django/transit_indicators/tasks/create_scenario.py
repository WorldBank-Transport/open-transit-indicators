import json
from time import sleep

from celery.utils.log import get_task_logger
from django.conf import settings
import httplib
import requests

from transit_indicators.models import Scenario
from userdata.models import OTIUser

logger = get_task_logger(__name__)

def run_scenario_creation(scenario):
    period = scenario.sample_period
    logger.debug('Starting scenario creation: %s for user %s, period %s, db_name: %s',
                 scenario.name, scenario.created_by.username, period.type, scenario.db_name)

    scenario.job_status = Scenario.StatusChoices.PROCESSING
    scenario.save()

    # A base scenario is optional. If one isn't defined, use the default database name
    default_db_name = settings.DATABASES['default']['NAME']
    base_db_name = scenario.base_scenario.name if scenario.base_scenario else 'transit_indicators'

    headers = {'Content-type': 'application/json', 'Accept': 'application/json'}
    token = OTIUser.objects.get(username='oti-admin').auth_token.key
    period = scenario.sample_period
    payload = json.dumps({
        'token': token,
        'db_name': scenario.db_name,
        'base_db_name': base_db_name,
        'sample_period': {
            'id': period.id,
            'type': period.type,
            'period_start': period.period_start.isoformat(),
            'period_end': period.period_end.isoformat(),
        }
    })

    logger.debug('Payload JSON: %s ', payload)
    response = requests.post(settings.SCALA_ENDPOINTS['SCENARIOS'], data=payload, headers=headers)

    if response.status_code != httplib.ACCEPTED:
        logger.error('%d encountered', response.status_code)
        logger.error(response.text)
        scenario.job_status = Scenario.StatusChoices.ERROR
        scenario.save()
        response.raise_for_status()
    else:
        logger.info('Response: %s ', response.json())
        logger.info('Scenario creation triggered for scenario: %s', scenario.name)

    # Re-query to get object status
    scenario = Scenario.objects.get(pk=scenario.pk)
    status = scenario.job_status

    # Wait for job status to change from processing before finishing job
    while status == Scenario.StatusChoices.PROCESSING:
        sleep(10)
        scenario = Scenario.objects.get(pk=scenario.pk)
        status = scenario.job_status
        logger.info('Checking job status for scenario creation: %s', scenario.name)

    if status == Scenario.StatusChoices.COMPLETE:
        logger.info('Scenario created successfully, db_name: %s', scenario.db_name)
    else:
        logger.info('Scenario creation failed with status: %s', status)
