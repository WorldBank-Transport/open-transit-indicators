from django.test import TestCase
from rest_framework.reverse import reverse
from rest_framework import status

from transit_indicators.tests import OTIAPIClient
from userdata.models import OTIUser


class OTIUserTestCase(TestCase):
    """ Tests behavior of OTIUser model."""
    def setUp(self):
        self.client = OTIAPIClient()
        self.list_url = reverse('users-list', {})
        self.data = {'username': 'test-1',
                     'password': '123',
                     'email': 'test@test.me',
                     'first_name': '',
                     'last_name': '',
                     'is_staff': False,
                     'is_active': True,
                     'is_superuser': False}
        # Password and is_superuser not returned by response
        self.response_data = {key: self.data[key] for key in self.data 
                              if key not in ['is_superuser','password']}

    def test_users_crud(self):
        """Test admin user CRUD operations on OTIUser

        Admin user should have full read/write permissions

        """
        self.client.authenticate(admin=True)
        num_users = OTIUser.objects.count()

        # CREATE
        response = self.client.post(self.list_url, self.data, format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertDictContainsSubset(self.response_data, response.data)
        user_id = response.data['id']
        detail_url = reverse('users-detail', [user_id])

        # READ
        response = self.client.get(detail_url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertDictContainsSubset(self.response_data, response.data)

        # UPDATE
        new_first_name = 'Jerry'
        patch_data = dict(first_name=new_first_name)
        response = self.client.patch(detail_url, patch_data, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(new_first_name, response.data['first_name'])

        # DELETE
        response = self.client.delete(detail_url)
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertEqual(0, OTIUser.objects.filter(id=user_id).count())

    def test_permissions(self):
        """Test CRUD operation permissions on OTIUser

        Standard user should only have read permissions
        Anonymous user should have no permissions

        """
        test_user = OTIUser.objects.create_user('test-1', password='123', email='test@test.me')
        user_id = test_user.id 
        detail_url = reverse('users-detail', [user_id])

        # Standard User
        self.client.authenticate(admin=False)

        # CREATE
        response = self.client.post(self.list_url, self.data, format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

        # READ
        response = self.client.get(detail_url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertDictContainsSubset(self.response_data, response.data)

        # UPDATE
        new_first_name = 'Jerry'
        patch_data = dict(first_name=new_first_name)
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
        new_first_name = 'Jerry'
        patch_data = dict(first_name=new_first_name)
        response = self.client.patch(detail_url, patch_data, format='json')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

        # DELETE
        response = self.client.delete(detail_url)
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)
