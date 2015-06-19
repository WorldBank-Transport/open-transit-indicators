from django.conf.urls import patterns, include, url

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
router.register(r'real-time', datasourcesviews.RealTimeViewSet, base_name='real-time')
router.register(r'real-time-problems', datasourcesviews.RealTimeProblemViewSet, base_name='real-time-problems')
router.register(r'osm-data', datasourcesviews.OSMDataViewSet, base_name='osm-data')
router.register(r'osm-data-problems', datasourcesviews.OSMDataProblemsViewSet, base_name='osm-data-problems')
router.register(r'users', userviews.OTIUserViewSet, base_name='users')
router.register(r'config', indicatorviews.OTIIndicatorsConfigViewSet, base_name='config')
router.register(r'config-demographic', indicatorviews.OTIDemographicConfigViewSet)
router.register(r'sample-periods', indicatorviews.SamplePeriodViewSet, base_name='sample-periods')
router.register(r'indicators', indicatorviews.IndicatorViewSet)
router.register(r'indicator-jobs', indicatorviews.IndicatorJobViewSet, base_name='indicator-jobs')
router.register(r'scenarios', indicatorviews.ScenarioViewSet)

urlpatterns = patterns('',  # NOQA
    url(r'^api/', include(router.urls)),
    url(r'^api-auth/', include('rest_framework.urls', namespace='rest_framework')),
    url(r'^api-token-auth/', 'userdata.views.obtain_auth_token'),
    url(r'^api/latest-calculation-job/', 'transit_indicators.views.latest_calculation'),
    url(r'^api/indicator-calculation-job/', 'transit_indicators.views.indicator_calculation_job'),
    url(r'^api/indicator-types/', 'transit_indicators.views.indicator_types'),
    url(r'^api/indicator-aggregation-types/', 'transit_indicators.views.indicator_aggregation_types'),
    url(r'^api/indicator-cities/', 'transit_indicators.views.indicator_cities'),
    url(r'^api/city-name/', 'transit_indicators.views.city_name'),
    url(r'^api/languages/', 'transit_indicators.views.available_languages'),
    url(r'^api/timezones/', 'transit_indicators.views.available_timezones'),
    url(r'^api/sample-period-types/', 'transit_indicators.views.sample_period_types'),
    url(r'^api/gtfs-route-types/', 'transit_indicators.views.gtfs_route_types'),
    url(r'^api/upload-statuses/', 'datasources.views.upload_status_choices'),
    url(r'^api/demographics-ranges/', 'datasources.views.demographic_data_ranges'),
)
