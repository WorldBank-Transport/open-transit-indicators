from django.conf.urls import patterns, include, url
from django.contrib import admin

from rest_framework.routers import DefaultRouter

from datasources import views as datasourcesviews
from userdata import views as userviews
import views as indicatorviews

router = DefaultRouter()
router.register(r'gtfs-feeds', datasourcesviews.GTFSFeedViewSet)
router.register(r'gtfs-feed-problems', datasourcesviews.GTFSFeedProblemViewSet)
router.register(r'boundaries', datasourcesviews.BoundaryViewSet)
router.register(r'boundary-problems', datasourcesviews.BoundaryProblemViewSet)
router.register(r'demographics', datasourcesviews.DemographicDataSourceViewSet)
router.register(r'demographics-features', datasourcesviews.DemographicDataFeatureViewSet)
router.register(r'demographics-problems', datasourcesviews.DemographicDataSourceProblemViewSet)
router.register(r'users', userviews.OTIUserViewSet)
router.register(r'config', indicatorviews.OTIIndicatorsConfigViewSet, base_name='config')
router.register(r'config-demographic', indicatorviews.OTIDemographicConfigViewSet)
router.register(r'peak-travel', indicatorviews.PeakTravelPeriodViewSet, base_name='peak-travel')

urlpatterns = patterns('',  # NOQA
    url(r'^api/', include(router.urls)),
    url(r'^api-auth/', include('rest_framework.urls', namespace='rest_framework')),

    url(r'^api-token-auth/', 'userdata.views.obtain_auth_token')
)
