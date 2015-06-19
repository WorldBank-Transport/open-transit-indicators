from datetime import datetime
import random
import string
import pytz

from rest_framework import status
from rest_framework.authtoken.views import ObtainAuthToken
from rest_framework.authtoken.models import Token
from rest_framework.decorators import action
from rest_framework.exceptions import PermissionDenied, ParseError
from rest_framework.response import Response

from transit_indicators.viewsets import OTIAdminViewSet

from userdata.models import OTIUser
from userdata.serializers import OTIUserSerializer


LOGIN_FLAG_DATE = pytz.utc.localize(datetime(2000, 01, 01))


class OTIUserViewSet(OTIAdminViewSet):
    """ This endpoint represents users in the system """
    model = OTIUser
    serializer_class = OTIUserSerializer
    filter_fields = ('username', 'email', 'first_name', 'last_name',)

    def create(self, request):
        """Override create to add setting of password"""
        response = super(OTIUserViewSet,self).create(request)
        if response.status_code == status.HTTP_201_CREATED:
            self.object.set_password(request.DATA.get('password'))
            self.object.save()
        return response

    @action()
    def reset_password(self, request, pk=None):
        """Endpoint for resetting a user's password"""
        user = self.get_object()

        # Generate random 10 letter + digit password
        all_chars = string.ascii_letters + string.digits
        new_password = ''.join(random.choice(all_chars) for _ in range(10))
        user.set_password(new_password)
        user.save()

        serializer = self.serializer_class(user)
        data = serializer.data
        data['password'] = new_password

        return Response(data)

    @action()
    def change_password(self, request, pk=None):
        """Endpoint for changing a user's password"""
        user = self.get_object()

        request_user = request.user

        if request_user != user and not request.user.is_staff:
            raise PermissionDenied(detail='Unable to set another user\'s password')

        new_password = request.DATA.get('password')
        if not new_password:
            raise ParseError(detail='Must supply a password to change to')
        user.set_password(new_password)
        user.save()

        return Response({'detail': 'password successfully changed'})




class OTIObtainAuthToken(ObtainAuthToken):
    """ Endpoint for acquiring an auth token for given username """
    def post(self, request):
        serializer = self.serializer_class(data=request.DATA)
        if serializer.is_valid():
            user = serializer.object['user']

            token, created = Token.objects.get_or_create(user=user)

            # check if this is the first time user has logged in
            userObject = OTIUser.objects.get(username=user)
            first_login = False
            if userObject and userObject.last_login < LOGIN_FLAG_DATE:
                first_login = True

            # set last login to now (does not get updated when token is fetched)
            userObject.last_login = datetime.utcnow()
            userObject.save()

            return Response({'token': token.key, 'user': token.user_id, 'firstLogin': first_login})

        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

obtain_auth_token = OTIObtainAuthToken.as_view()
