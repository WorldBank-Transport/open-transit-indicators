from datetime import datetime, time, timedelta

from django.test import TestCase
from rest_framework.reverse import reverse
from rest_framework.test import APIClient
from rest_framework.authtoken.models import Token
from rest_framework import status

from transit_indicators.models import OTIIndicatorsConfig

from userdata.models import OTIUser

class OTIAPIClient(APIClient):

    def __init__(self, enforce_csrf_checks=False, **defaults):
        super(OTIAPIClient, self).__init__(enforce_csrf_checks=enforce_csrf_checks, **defaults)
        self.api_user = self.get_test_user(is_admin=False)
        self.api_admin = self.get_test_user(is_admin=True)
        self.authenticate(admin=True)

    def get_test_user(self, is_admin=False):
        """Create a test user and generate a token

        @param is_super: True if the user should be a django superuser

        """
        username = 'test_admin' if is_admin else 'test_user'
        password = '123'
        email = 'test@test.me'
        user = None
        try:
            user = OTIUser.objects.get(username=username)
        except OTIUser.DoesNotExist:
            if is_admin:
                user = OTIUser.objects.create_superuser(username, email, password)
            else:
                user = OTIUser.objects.create_user(username, password=password, email=email)
        token, created = Token.objects.get_or_create(user=user)
        return user

    def authenticate(self, admin=False):
        user = self.api_admin if admin else self.api_user
        self.force_authenticate(user=user, token=user.auth_token)


class OTIIndicatorsConfigTestCase(TestCase):
    """ Tests behavior of OTIIndicatorsConfig model."""
    def setUp(self):
        self.client = OTIAPIClient()
        self.list_url = reverse('config-list', {})
        self.data = {'poverty_line': 256.36,
                     'nearby_buffer_distance_m': 500.0,
                     'max_commute_time_s': 3600,
                     'max_walk_time_s': 600,
                     'avg_fare': 500}

    def test_config_crud(self):
        """Test admin user CRUD operations on OTIIndicatorsConfig.

        Admin user should have full read/write permissions

        """
        self.client.authenticate(admin=True)

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

    def test_permissions(self):
        """Test CRUD operation permissions on OTIIndicatorsConfig.

        Standard user should only have read permissions
        Anonymous user should have no permissions

        """
        self.client.authenticate(admin=True)

        # CREATE
        response = self.client.post(self.list_url, self.data, format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertDictContainsSubset(self.data, response.data)
        config_id = response.data['id']
        detail_url = reverse('config-detail', [config_id])


        # Standard User
        self.client.authenticate(admin=False)

        # CREATE
        response = self.client.post(self.list_url, self.data, format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

        # READ
        response = self.client.get(detail_url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertDictContainsSubset(self.data, response.data)

        # UPDATE
        new_max_walk_time = 300
        patch_data = dict(max_walk_time_s=new_max_walk_time)
        response = self.client.patch(detail_url, patch_data, format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

        # DELETE
        response = self.client.delete(detail_url)
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

        # Anonymous user
        self.client.force_authenticate(user=None)

        # CREATE
        response = self.client.post(self.list_url, self.data, format='json')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

        # READ
        response = self.client.get(detail_url)
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

        # UPDATE
        new_max_walk_time = 300
        patch_data = dict(max_walk_time_s=new_max_walk_time)
        response = self.client.patch(detail_url, patch_data, format='json')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

        # DELETE
        response = self.client.delete(detail_url)
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)


    def test_config_values(self):
        """ Test that bad values aren't accepted."""
        def check_negative_number(data, key):
            """ Inserts a negative number into the data at data[key] and then POSTs to self.list_url"""
            bad_data = dict(data)
            bad_data[key] = -10
            response = self.client.post(self.list_url, bad_data, format='json')
            self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

        self.client.authenticate(admin=True)
        check_negative_number(self.data, 'poverty_line')
        check_negative_number(self.data, 'avg_fare')
        check_negative_number(self.data, 'nearby_buffer_distance_m')
        check_negative_number(self.data, 'max_commute_time_s')
        check_negative_number(self.data, 'max_walk_time_s')


class PeakTravelPeriodTestCase(TestCase):
    """ Tests PeakTravelPeriods """
    def setUp(self):
        self.client = OTIAPIClient()
        self.client.authenticate(admin=True)
        self.list_url = reverse('peak-travel-list', {})

    def test_start_after_end(self):
        """ Ensure that PeakTravelPeriods can't end before they start. """
        before = time(hour=12)
        after = time(hour=13)

        response = self.client.post(self.list_url, dict(start_time=before,
                                                        end_time=after),
                                    format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

        response = self.client.post(self.list_url, dict(start_time=after,
                                                        end_time=before),
                                    format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_permissions(self):
        self.client.authenticate(admin=False)
        now = time(hour=12)
        later = time(hour=13)

        response = self.client.post(self.list_url, dict(start_time=now,
                                                        end_time=later),
                                    format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

        self.client.force_authenticate(user=None)
        response = self.client.post(self.list_url, dict(start_time=now,
                                                        end_time=later),
                                    format='json')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)
