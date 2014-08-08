from datetime import datetime, time, timedelta
import os

from django.test import TestCase
from django.utils.timezone import utc
from models import SamplePeriod
from rest_framework.reverse import reverse
from rest_framework.test import APIClient
from rest_framework.authtoken.models import Token
from rest_framework import status

from transit_indicators.models import OTIIndicatorsConfig, Indicator

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


class SamplePeriodsTestCase(TestCase):
    """Tests SamplePeriods"""
    def setUp(self):
        self.client = OTIAPIClient()
        self.client.authenticate(admin=True)
        self.list_url = reverse('sample-periods-list', {})

    def test_create(self):
        """Ensure that a valid SamplePeriod can be created."""
        start = datetime(2000, 1, 1, 0, 0, 0, tzinfo=utc)
        end = datetime(2000, 1, 1, 12, 0, 0, tzinfo=utc)
        period_type = 'night'
        response = self.client.post(self.list_url, dict(period_start=start,
                                                        period_end=end,
                                                        type=period_type),
                                    format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

    def test_start_after_end(self):
        """Ensure that SamplePeriods can't end before they start."""
        start = datetime(2000, 1, 1, 0, 0, 0, tzinfo=utc)
        end = datetime(2000, 1, 1, 12, 0, 0, tzinfo=utc)
        period_type = 'night'
        response = self.client.post(self.list_url, dict(period_start=end,
                                                        period_end=start,
                                                        type=period_type),
                                    format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_over_one_day(self):
        """Ensure that SamplePeriods can't be longer than a day."""
        start = datetime(2000, 1, 1, 0, 0, 0, tzinfo=utc)
        end = datetime(2000, 1, 10, 0, 0, 0, tzinfo=utc)
        period_type = 'night'

        response = self.client.post(self.list_url, dict(period_start=end,
                                                        period_end=start,
                                                        type=period_type),
                                    format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)


class IndicatorsTestCase(TestCase):
    """Tests Indicators"""
    def setUp(self):
        self.client = OTIAPIClient()
        self.client.authenticate(admin=True)
        self.list_url = reverse('indicator-list', {})

        # initialize a sample_period
        start = datetime(2000, 1, 1, 0, 0, 0, tzinfo=utc)
        end = datetime(2000, 1, 1, 12, 0, 0, tzinfo=utc)
        self.sample_period = SamplePeriod.objects.create(type='morning',
                                                         period_start=start, period_end=end)

    def test_create(self):
        """Ensure that a valid Indicator can be created."""
        response = self.client.post(self.list_url, dict(sample_period=self.sample_period.type,
                                                        type='num_stops',
                                                        aggregation='system',
                                                        value=100),
                                    format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

    def test_filter(self):
        """Ensure that the local_city filter only returns city_name == null"""
        indicator = Indicator.objects.create(sample_period=self.sample_period,
                                             type=Indicator.IndicatorTypes.NUM_ROUTES,
                                             aggregation=Indicator.AggregationTypes.SYSTEM,
                                             city_bounded=True,
                                             version=1,
                                             value=42)
        indicator.save()
        indicator2 = Indicator.objects.create(sample_period=self.sample_period,
                                             type=Indicator.IndicatorTypes.NUM_ROUTES,
                                             aggregation=Indicator.AggregationTypes.SYSTEM,
                                             city_bounded=True,
                                             version=1,
                                             value=42,
                                             city_name="Rivendell")
        indicator2.save()

        response = self.client.get(self.list_url, data={ 'local_city': 'True' })
        self.assertEqual(len(response.data), 1)
        self.assertIsNone(response.data[0]['city_name'])


    def test_csv(self):
        """Ensure that indicators can be dumped via csv"""

        indicator = Indicator.objects.create(sample_period=self.sample_period,
                                             type=Indicator.IndicatorTypes.NUM_ROUTES,
                                             aggregation=Indicator.AggregationTypes.SYSTEM,
                                             city_bounded=True,
                                             version=1,
                                             value=42)
        indicator.save()

        # On get requests, format parameter gets passed to the data object,
        # On any other type of request, its a named argument: get(url, data, format='csv')
        response = self.client.get(self.list_url, data={ 'format': 'csv' })
        csv_response = 'aggregation,city_bounded,city_name,route_id,route_type,sample_period,type,value,version\r\n'
        csv_response += 'system,True,,,,morning,num_routes,42.0,1\r\n'
        self.assertEqual(response.content, csv_response)

    def test_csv_import(self):
        """Ensure csv import endpoint requires city_name parameter

        Test csv file must have sample_period == self.sample_period.type in order to succeed

        """
        file_directory = os.path.dirname(os.path.abspath(__file__))
        test_csv = open(file_directory + '/tests/test_indicators.csv', 'rb')
        self.sample_period.save()

        response = self.client.post(self.list_url, {'city_name': 'Rivendell', 'source_file': test_csv})
        self.assertEqual(response.status_code, status.HTTP_200_OK)

        response = self.client.post(self.list_url, {'source_file': test_csv})
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)


    def test_bad_sample_period(self):
        """A bad value for the sample_period should cause a failure."""
        response = self.client.post(self.list_url, dict(sample_period=-1,
                                                        type='num_stops',
                                                        aggregation='system',
                                                        value=100),
                                    format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_bad_type(self):
        """A bad indicator type should cause a failure."""
        response = self.client.post(self.list_url, dict(sample_period=self.sample_period.type,
                                                        type='bad_type',
                                                        aggregation='system',
                                                        value=100),
                                    format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_bad_type(self):
        """A bad aggregation should cause a failure."""
        response = self.client.post(self.list_url, dict(sample_period=self.sample_period.type,
                                                        type='num_stops',
                                                        aggregation='bad_agg',
                                                        value=100),
                                    format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_route_aggregation(self):
        """Test an indicator with a route aggregation."""

        # Good: a route aggregation with a route_id and no route_type
        response = self.client.post(self.list_url, dict(sample_period=self.sample_period.type,
                                                        type='num_stops',
                                                        aggregation='route',
                                                        route_id='ABC',
                                                        value=100),
                                    format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

        # Bad: a route aggregation with no route_id
        response = self.client.post(self.list_url, dict(sample_period=self.sample_period.type,
                                                        type='num_stops',
                                                        aggregation='route',
                                                        value=100),
                                    format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

        # Bad: a route aggregation with a route_id and a route_type
        response = self.client.post(self.list_url, dict(sample_period=self.sample_period.type,
                                                        type='num_stops',
                                                        aggregation='route',
                                                        route_id='ABC',
                                                        route_type=1,
                                                        value=100),
                                    format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_mode_aggregation(self):
        """Test an indicator with a mode aggregation."""

        # Good: a mode aggregation with a route_type and no route_id
        response = self.client.post(self.list_url, dict(sample_period=self.sample_period.type,
                                                        type='num_stops',
                                                        aggregation='mode',
                                                        route_type=1,
                                                        value=100),
                                    format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

        # Bad: a mode aggregation with no route_type
        response = self.client.post(self.list_url, dict(sample_period=self.sample_period.type,
                                                        type='num_stops',
                                                        aggregation='mode',
                                                        value=100),
                                    format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

        # Bad: a mode aggregation with a route_id and a route_type
        response = self.client.post(self.list_url, dict(sample_period=self.sample_period.type,
                                                        type='num_stops',
                                                        aggregation='mode',
                                                        route_id='ABC',
                                                        route_type=1,
                                                        value=100),
                                    format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
