from django.conf.urls import patterns, include, url
from django.contrib import admin

from rest_framework.routers import DefaultRouter

from datasources import views as datasourcesviews
from userdata import views as userviews
import views as indicatorviews

router = DefaultRouter()
router.register(r'gtfs-feeds', datasourcesviews.GTFSFeedViewSet)
router.register(r'gtfs-feed-problems', datasourcesviews.GTFSFeedProblemViewSet)
router.register(r'users', userviews.OTIUserViewSet)
router.register(r'config', indicatorviews.OTIIndicatorsConfigViewSet)
router.register(r'peak-times', indicatorviews.PeakTravelPeriodViewSet)

urlpatterns = patterns('',  # NOQA
    url(r'^admin/', include(admin.site.urls)),

    url(r'^api/', include(router.urls)),
    url(r'^api-auth/', include('rest_framework.urls', namespace='rest_framework')),

    url(r'^api-token-auth/', 'userdata.views.obtain_auth_token')
)
