from rest_framework.serializers import ModelSerializer

from userdata.models import OTIUser

class OTIUserSerializer(ModelSerializer):
    """ Custom serializaition for OTIUser objects """

    class Meta(object):
        model = OTIUser
        read_only_fields = ('id', 'last_login', 'date_joined')
        write_only_fields = ('password',)
        exclude = ('is_superuser', 'groups', 'user_permissions')
