from shutil import rmtree
import tempfile

from django.test import TestCase

from rest_framework import status
from rest_framework.reverse import reverse
from rest_framework.test import APIClient


class GTFSFeedTestCase(TestCase):
    """Tests behavior of GTFSFeed models. """

    def setUp(self):
        self.client = APIClient()
        self.url = reverse('gtfsfeed-list', {})

    def test_zip_extension(self):
        """Test that uploaded files are correctly checked for .zip extensions."""
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

        rmtree(temp_dir)
