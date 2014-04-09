from os import path
from shutil import rmtree
import tempfile
from time import sleep

from django.conf import settings
from django.test import TestCase
from django.test.utils import override_settings

from rest_framework import status
from rest_framework.reverse import reverse
from rest_framework.test import APIClient


class GTFSFeedTestCase(TestCase):
    """Tests behavior of GTFSFeed models. """

    def setUp(self):
        self.client = APIClient()
        self.url = reverse('gtfsfeed-list', {})

    @override_settings(CELERY_EAGER_PROPAGATES_EXCEPTIONS=True,
                       CELERY_ALWAYS_EAGER=True,
                       BROKER_BACKEND='memory')
    def test_zip_extension_validation(self):
        """Test that uploaded files are correctly checked for .zip extensions and validated."""
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

        with open(goodfile_path, 'r') as goodfile:
            # Test that uploading a file with a .zip extension succeeds
            response = self.client.post(self.url, {'source_file': goodfile})
            self.assertEqual(response.status_code, status.HTTP_201_CREATED,
                             response.content)
                             
            # get the ID for this feed, to check that validation gets run on it
            feed_id = str(response.data['id'])
            sleep(2)  # wait for validator to run
            # Test that validation output exists for uploaded file
            response = self.client.get(self.url, {'id': feed_id})
            validation_file = response.data[0]['validation_results_file']
            
            # check that the validation output file location has been saved to the model
            self.assertNotEqual(len(validation_file), 0)
            
            # check that the validation output file exists on disk
            validation_path = path.join(settings.MEDIA_ROOT, path.normpath(validation_file))
            self.assertTrue(path.isfile(validation_path))

        rmtree(temp_dir)
