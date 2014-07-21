from datetime import datetime, timedelta

from django.test import TestCase
from rest_framework.reverse import reverse
from rest_framework.test import APIClient
from rest_framework import status

from transit_indicators.models import OTIIndicatorsConfig


class OTIIndicatorsConfigTestCase(TestCase):
    """ Tests behavior of OTIIndicatorsConfig model."""
    def setUp(self):
        self.client = APIClient()
        self.list_url = reverse('config-list', {})
        self.data = {'poverty_line_usd': 256.36,
                     'nearby_buffer_distance_m': 500.0,
                     'max_commute_time_s': 3600,
                     'max_walk_time_s': 600}

    def test_config_crud(self):
        """Test CRUD operations on OTIIndicatorsConfig. """
        # CREATE
        response = self.client.post(self.list_url, self.data, format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertDictContainsSubset(self.data, response.data)
        config_id = response.data['id']
        detail_url = reverse('config-detail', [config_id])

        # READ
        response = self.client.get(detail_url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertDictContainsSubset(self.data, response.data)

        # UPDATE
        new_max_walk_time = 300
        patch_data = dict(max_walk_time_s=new_max_walk_time)
        response = self.client.patch(detail_url, patch_data, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(new_max_walk_time, response.data['max_walk_time_s'])

        # DELETE
        response = self.client.delete(detail_url)
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertEqual(0, OTIIndicatorsConfig.objects.filter(id=config_id).count())

    def test_config_values(self):
        """ Test that bad values aren't accepted."""
        def check_negative_number(data, key):
            """ Inserts a negative number into the data at data[key] and then POSTs to self.list_url"""
            bad_data = dict(data)
            bad_data[key] = -10
            response = self.client.post(self.list_url, bad_data, format='json')
            self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

        check_negative_number(self.data, 'poverty_line_usd')
        check_negative_number(self.data, 'nearby_buffer_distance_m')
        check_negative_number(self.data, 'max_commute_time_s')
        check_negative_number(self.data, 'max_walk_time_s')


class PeakTravelPeriodTestCase(TestCase):
    """ Tests PeakTravelPeriods """
    def setUp(self):
        self.client = APIClient()
        self.list_url = reverse('peak-travel-list', {})

    def test_start_after_end(self):
        """ Ensure that PeakTravelPeriods can't end before they start. """
        before = datetime.now()
        after = datetime.now() + timedelta(hours=1)

        response = self.client.post(self.list_url, dict(start_time=before.time(),
                                                        end_time=after.time()),
                                    format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

        response = self.client.post(self.list_url, dict(start_time=after.time(),
                                                        end_time=before.time()),
                                    format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
