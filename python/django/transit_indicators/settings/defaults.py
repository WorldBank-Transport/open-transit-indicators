"""
Django settings for transit_indicators project.

For more information on this file, see
https://docs.djangoproject.com/en/dev/topics/settings/

For the full list of settings and their values, see
https://docs.djangoproject.com/en/dev/ref/settings/
"""

# Build paths inside the project like this: os.path.join(BASE_DIR, ...)
import os

# Celery
from kombu import Exchange, Queue

# Import stuff created by the provision script
from secret_key import *
from provision_settings import *


BASE_DIR = os.path.dirname(os.path.dirname(__file__))


# See https://docs.djangoproject.com/en/dev/howto/deployment/checklist/

# SECURITY WARNING: don't run with debug turned on in production!
DEBUG = False

TEMPLATE_DEBUG = DEBUG

ALLOWED_HOSTS = ['*',]

# Application definition

AUTH_USER_MODEL = 'userdata.OTIUser'

INSTALLED_APPS = (
    'django.contrib.admin',
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.messages',
    'django.contrib.staticfiles',

    'rest_framework',
    'rest_framework.authtoken',
    'rest_framework_csv',
    'django_extensions',

    'datasources',
    'gtfs_realtime',
    'transit_indicators',
    'userdata',
)

MIDDLEWARE_CLASSES = (
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.middleware.locale.LocaleMiddleware',
    'django.middleware.common.CommonMiddleware',
    'django.middleware.csrf.CsrfViewMiddleware',
    'django.contrib.auth.middleware.AuthenticationMiddleware',
    'django.contrib.messages.middleware.MessageMiddleware',
    'django.middleware.clickjacking.XFrameOptionsMiddleware',
)

ROOT_URLCONF = 'transit_indicators.urls'

WSGI_APPLICATION = 'transit_indicators.wsgi.application'


# Internationalization
# https://docs.djangoproject.com/en/dev/topics/i18n/

LANGUAGE_CODE = 'en-us'

LANGUAGE_COOKIE_NAME = 'openTransitLanguage'

TIME_ZONE = 'UTC'

USE_I18N = True

USE_L10N = True

USE_TZ = True

LOCALE_PATHS = (
    BASE_DIR + '/i18n/',
)

# Static files (CSS, JavaScript, Images)
# https://docs.djangoproject.com/en/dev/howto/static-files/
STATIC_URL = '/static/'

STATIC_ROOT = '/projects/open-transit-indicators/static'

# Celery settings -- only use JSON for serialization
# Set because upstart runs as superuser and celery complains
# if we allow pickling of object as superuser
CELERY_TASK_SERIALIZER = 'json'
CELERY_RESULT_SERIALIZER = 'json'
CELERY_QUEUES = (
    Queue('datasources', Exchange('datasources'), routing_key='datasources'),
    Queue('indicators', Exchange('indicators'), routing_key='indicators')
)
CELERY_ROUTES = {
    'datasource_import_tasks': {'queue': 'datasources', 'routing_key': 'datasources'},
    'calculate_indicator_tasks': {'queue': 'indicators', 'routing_key': 'indicators'}
}
CELERY_ACCEPT_CONTENT=['json']

# Rest Framework Settings
REST_FRAMEWORK = {
    'DEFAULT_FILTER_BACKENDS': ('rest_framework.filters.DjangoFilterBackend',),
    'DEFAULT_AUTHENTICATION_CLASSES': (
        'rest_framework.authentication.TokenAuthentication',
        # Required for API browsing interface
        'rest_framework.authentication.SessionAuthentication',
        'rest_framework.authentication.BasicAuthentication'
    ),
    'DEFAULT_PERMISSIONS_CLASSES': ('rest_framework.permissions.IsAuthenticated',),
    'TEST_REQUEST_RENDERER_CLASSES': (
        'rest_framework.renderers.MultiPartRenderer',
        'rest_framework.renderers.JSONRenderer',
        'rest_framework_csv.renderers.CSVRenderer',
    ),
}

# Settings for Geometry data stored in database
DJANGO_SRID = 3857

# Temporary default city_name for indicators
OTI_CITY_NAME = 'My City'
