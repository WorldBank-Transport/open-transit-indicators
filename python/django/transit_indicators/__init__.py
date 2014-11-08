# This will make sure the app is always imported when
# Django starts so that shared_task will use this app.
from celery_settings import app as celery_app

default_app_config = 'transit_indicators.apps.TransitIndicatorsAppConfig'
