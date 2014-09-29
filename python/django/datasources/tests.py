import os
from shutil import rmtree
import tempfile
from time import sleep

from django.conf import settings
from django.test import TestCase
from django.test.utils import override_settings

from rest_framework import status
from rest_framework.reverse import reverse

from transit_indicators.tests import OTIAPIClient
from datasources.models import GTFSFeedProblem, RealTimeProblem

@override_settings(CELERY_EAGER_PROPAGATES_EXCEPTIONS=True,
                   CELERY_ALWAYS_EAGER=True,
                   BROKER_BACKEND='memory')
class GTFSFeedTestCase(TestCase):
    """Tests behavior of GTFSFeed models. """

    def setUp(self):
        self.client = OTIAPIClient()
        self.url = reverse('gtfsfeed-list', {})
        self.file_directory = os.path.dirname(os.path.abspath(__file__))
        self.test_gtfs_fh = open(self.file_directory + '/tests/test-gtfs.zip', 'rb')

    def tearDown(self):
        self.test_gtfs_fh.close()

    def test_zip_extension_validation(self):
        """Test that uploaded files are correctly checked for .zip extensions and validated."""
        self.client.authenticate(admin=True)
        temp_dir = tempfile.mkdtemp()
        badfile_path = temp_dir + '/badfile.jpg'
        goodfile_path = temp_dir + '/goodfile.zip'

        with open(badfile_path, 'w') as badfile:
            badfile.write("No useful data here.")

        with open(goodfile_path, 'w') as goodfile:
            goodfile.write("Useful data here.")

        with open(badfile_path, 'r') as badfile:
            # Test that uploading a file with an extension other than .zip
            # fails
            response = self.client.post(self.url, {'source_file': badfile})
            self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST,
                             response.content)

        rmtree(temp_dir)

    def test_gtfs_validation(self):
        """Test that verifies GTFS validation works correctly"""
        self.client.authenticate(admin=True)
        response = self.client.post(self.url, {'source_file': self.test_gtfs_fh})
        sleep(2) # give time for celery to do job
        problem_count = GTFSFeedProblem.objects.filter(gtfsfeed_id=response.data['id']).count()
        self.assertGreater(problem_count, 0, 'There should have been problems for uploaded data')

    def test_gtfs_validation_no_shapes(self):
        """Test that verifies GTFS validation works correctly"""
        self.client.authenticate(admin=True)
        with open(self.file_directory + '/tests/patco.zip', 'rb') as patco:
            response = self.client.post(self.url, {'source_file': patco})
        sleep(2) # give time for celery to do job
        problem_count = GTFSFeedProblem.objects.filter(gtfsfeed_id=response.data['id']).count()
        self.assertGreater(problem_count, 0, 'There should have been problems for uploaded data')

    def test_gtfs_upload_requires_admin(self):
        """Test that verifies GTFS upload requires an admin user"""
        # UNAUTHORIZED if no credentials
        self.client.force_authenticate(user=None)
        response = self.client.post(self.url, {'source_file': self.test_gtfs_fh})
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

        # FORBIDDEN if user is not superuser
        self.client.authenticate(admin=False)
        response = self.client.post(self.url, {'source_file': self.test_gtfs_fh})
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

class RealTimeTestCase(TestCase):

    def setUp(self):
        self.client = OTIAPIClient()
        self.url = reverse('real-time-list', {})
        self.file_directory = os.path.dirname(os.path.abspath(__file__))
        self.test_realtime_fh = open(self.file_directory + '/tests/stop_times_test.txt_new')

    def tearDown(self):
        self.test_realtime_fh.close()

    def test_txt_new_validation(self):
        self.client.authenticate(admin=True)

        temp_dir = tempfile.mkdtemp()
        badfile_path = temp_dir + '/badfile.txt'
        goodfile_path = temp_dir + '/goodfile.txt_new'

        with open(badfile_path, 'w') as badfile:
            badfile.write("No useful data here.")

        with open(goodfile_path, 'w') as goodfile:
            goodfile.write("Useful data here.")

        with open(badfile_path, 'r') as badfile:
            # Test that uploading a file with an extension other than .txt_new
            # fails
            response = self.client.post(self.url, {'source_file': badfile})
            self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST,
                             response.content)

        rmtree(temp_dir)

    def test_realtime_upload(self):
        response = self.client.post(self.url, {'source_file': self.test_realtime_fh,
                                               'city_name': settings.OTI_CITY_NAME})
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
