from rest_framework.filters import OrderingFilter

from transit_indicators.viewsets import OTIAdminViewSet

class FileDataSourceViewSet(OTIAdminViewSet):
    filter_fields = ('status', 'city_name',)
    ordering_fields = ('id', 'last_modify_date',)
    filter_backends = (OrderingFilter,)
