from rest_framework import status
from rest_framework.authtoken.views import ObtainAuthToken
from rest_framework.authtoken.models import Token
from rest_framework import permissions
from rest_framework.response import Response
from rest_framework.viewsets import ModelViewSet

from transit_indicators.viewsets import OTIAdminViewSet

from userdata.models import OTIUser
from userdata.serializers import OTIUserSerializer


class OTIUserViewSet(OTIAdminViewSet):
    """ This endpoint represents users in the system """
    model = OTIUser
    serializer_class = OTIUserSerializer
    filter_fields = ('username', 'email', 'first_name', 'last_name',)


class OTIObtainAuthToken(ObtainAuthToken):
    """ Endpoint for acquiring an auth token for given username """
    def post(self, request):
        serializer = self.serializer_class(data=request.DATA)
        if serializer.is_valid():
            user = serializer.object['user']
            token, created = Token.objects.get_or_create(user=user)
            return Response({'token': token.key, 'user': token.user_id})

        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

obtain_auth_token = OTIObtainAuthToken.as_view()
