"""
Management command to trigger the calculation of indicators

This command will probably only be used for testing. It demonstrates how to make
a request to GeoTrellis that calculates all of the indicators.
"""

import json
import logging
import time
import requests
from django.core.management.base import BaseCommand
from transit_indicators.models import SamplePeriod
from userdata.models import OTIUser

logging.basicConfig(format='%(asctime)s, %(name)s [%(levelname)s] %(message)s',
                    level=logging.DEBUG)
LOGGER = logging.getLogger(__name__)


class Command(BaseCommand):
    """Command to trigger the calculation of indicators"""
    args = None
    help = 'Triggers the calculation of indicators'
    GT_INDICATORS_ENDPOINT = 'http://localhost/gt/indicators'

    def handle(self, *args, **options):
        """Method that handles triggering calculation of indicators"""

        # get the version -- versioning doesn't exist yet, just use a timestamp
        version = int(round(time.time()))

        # get a token to give to GT, so it can post indicators as they're calculated
        token = OTIUser.objects.get(username='oti-admin').auth_token.key

        # get the sample periods
        sample_periods = [
            {
                'id': s.id,
                'type': s.type,
                'period_start': s.period_start.isoformat(),
                'period_end': s.period_end.isoformat(),
            }
            for s in SamplePeriod.objects.exclude(type="alltime")
        ]

        # construct the payload
        payload = json.dumps({
            'version': version,
            'token': token,
            'sample_periods': sample_periods,
        })
        LOGGER.debug('Payload JSON: %s ', payload)

        # post to GT endpoint to start indicators calculating
        headers = {'Content-type': 'application/json', 'Accept': 'application/json'}
        response = requests.post(self.GT_INDICATORS_ENDPOINT, data=payload, headers=headers)
        if response.status_code != 201:
            LOGGER.error('%d encountered', response.status_code)
            LOGGER.error(response.text)
        else:
            LOGGER.info('Response: %s ', response.json())
            LOGGER.info('Calculations triggered for version: %i', version)
