#!/bin/bash
PROJECTS_DIR="/projects"

# Set the path to the project directory; all other paths will be relative to this.
PROJECT_ROOT="$PROJECTS_DIR/open-transit-indicators"

# Abort on error. If you want to ignore errors for command, use command || true
set -o errexit

# Set up Vagrant box for World Bank Transit Planning tools. Should be fairly similar
# to the actual install script.

# Needs to install software, so root privileges required.
if [ `whoami` != "root" ]; then
    echo "This installation script must be run as root; please use 'sudo provision.sh'" >&2
    exit 1
fi
if [ $# -eq 0 ]; then
    echo 'Must provide an installation type, e.g. development. Aborting.' >&2
    exit 1
fi

# Set the path to the project directory; all other paths will be relative to this.
INSTALL_TYPE=$1
if [ "$INSTALL_TYPE" == "travis" ]; then
    # For Travis, we start in current directory.
    CURDIR=`pwd`
    PROJECT_ROOT=`dirname $CURDIR`
    PROJECTS_DIR='/home/travis/build'
else
    PROJECT_ROOT="/projects/open-transit-indicators"
fi

echo "Using project root: $PROJECT_ROOT"

#########################################
# Installation configuration parameters #
#########################################

HOST='127.0.0.1'  # TODO: set for production / travis

TEMP_ROOT='/tmp'
DJANGO_ROOT="$PROJECT_ROOT/python/django"
DJANGO_STATIC_FILES_ROOT="$PROJECT_ROOT/static"
GEOTRELLIS_ROOT="$PROJECT_ROOT/geotrellis"

# Additional repos for snapshot versions of GeoTrellis and GTFS parser.
# The changes in these repos haven't been published to Maven and may
# not be for quite a while, so we need to publish them locally.
GEOTRELLIS_REPO_ROOT="$PROJECTS_DIR/geotrellis"
GEOTRELLIS_REPO_URI="https://github.com/geotrellis/geotrellis.git"
GEOTRELLIS_REPO_BRANCH="master"
GTFS_PARSER_REPO_ROOT="$PROJECTS_DIR/gtfs-parser"
GTFS_PARSER_REPO_URI="https://github.com/echeipesh/gtfs-parser.git"
GTFS_PARSER_REPO_BRANCH="feature/slick"

UPLOADS_ROOT='/var/local/transit-indicators-uploads' # Storage for user-uploaded files
ANGULAR_ROOT="$PROJECT_ROOT/js/angular"
WINDSHAFT_ROOT="$PROJECT_ROOT/js/windshaft"
LOG_ROOT="$PROJECT_ROOT/logs"
WEB_USER='vagrant' # User under which web service runs.

DB_NAME="transit_indicators"
DB_PASS=$DB_NAME
DB_USER=$DB_NAME
DB_HOST=$HOST
DB_PORT='5432'
VHOST_NAME=$DB_NAME

REDIS_HOST=$HOST
REDIS_PORT='6379'
VHOST_NAME=$DB_NAME

GEOTRELLIS_PORT=8001
GEOTRELLIS_HOST="http://127.0.0.1:$GEOTRELLIS_PORT"
GEOTRELLIS_CATALOG="data/catalog.json"
RABBIT_MQ_HOST="127.0.0.1"
RABBIT_MQ_PORT="5672"

APP_SU_USERNAME="oti-user"
APP_SU_PASSWORD=$APP_SU_USERNAME
# TODO: Change this?
APP_SU_EMAIL="$APP_SU_USERNAME@azavea.com"

WINDSHAFT_PORT=4000
WINDSHAFT_HOST="http://localhost:$WINDSHAFT_PORT"

GUNICORN_WORKERS=3

# Create logs directory
mkdir -p $LOG_ROOT

# Set the install type. Should be one of [development|production|travis].
case "$INSTALL_TYPE" in
    "development")
        echo "Selecting development installation"
        WEB_USER='vagrant' # User under which web service runs.
        ANGULAR_STATIC="$ANGULAR_ROOT/app"
        GUNICORN_MAX_REQUESTS="--max-requests 1" # force gunicorn to reload code
        ;;
    "production")
        echo "Selecting production installation"
        ANGULAR_STATIC="$ANGULAR_ROOT/dist"
        GUNICORN_MAX_REQUESTS=""
        # TODO: Set variables for production deployment here
        ;;
    "travis")
        echo "Selecting CI installation"
        WEB_USER='travis' # User under which web service runs.
        ANGULAR_STATIC="$ANGULAR_ROOT/app"
        ;;
    *)
        echo "Invalid installation type; should be one of development / production / travis" >&2
        exit 1
        ;;
esac


#########################
# Project Dependencies  #
#########################
apt-get -qq update
# Make add-apt-repository available

add-apt-repository -y ppa:mapnik/v2.2.0
add-apt-repository -y ppa:gunicorn/ppa
add-apt-repository -y ppa:chris-lea/node.js

if [ "$INSTALL_TYPE" == "travis" ]; then
    echo "Installing packages for Travis..."
    # Travis CI already has many packages installed;
    # attempting to install PostgreSQL/PostGIS here breaks things.
    apt-get -qq update
    
    apt-get -y -qq install \
        nodejs \
        libxml2-dev libxslt1-dev python-all-dev \
        postgresql-server-dev-9.1 \
        build-essential libproj-dev libjson0-dev xsltproc docbook-xsl docbook-mathml \
        libmapnik libmapnik-dev python-mapnik mapnik-utils \
        nginx \
        gunicorn \
        > /dev/null  # silence, package manager!  only show output on error
else
    # non-CI build; install All the Things
    add-apt-repository -y "deb http://www.rabbitmq.com/debian/ testing main"

    # add public key for RabbitMQ
    pushd $TEMP_ROOT
        wget http://www.rabbitmq.com/rabbitmq-signing-key-public.asc
        apt-key add rabbitmq-signing-key-public.asc
        rm rabbitmq-signing-key-public.asc
    popd

    # Install dependencies available via apt
    # Lines roughly grouped by functionality (e.g. Postgres, python, node, etc.)
    apt-get update
    
    # also install dependencies for building postgis
    apt-get -y install \
        git htop multitail \
        python-pip python-dev python-all-dev  \
        libxml2-dev libxslt1-dev \
        build-essential libproj-dev libjson0-dev xsltproc docbook-xsl docbook-mathml \
        postgresql-9.1 postgresql-server-dev-9.1 \
        nodejs \
        ruby1.9.3 rubygems \
        openjdk-7-jre openjdk-7-jdk scala \
        rabbitmq-server \
        libmapnik libmapnik-dev python-mapnik mapnik-utils redis-server \
        nginx \
        gunicorn
fi

#### build postgis and friends #############
# http://trac.osgeo.org/postgis/wiki/UsersWikiPostGIS21Ubuntu1204src

if type shp2pgsql 2>/dev/null; then
    echo 'PostGIS already installed; skipping.'
elif [ "$INSTALL_TYPE" != "travis" ]; then
    pushd $TEMP_ROOT
        # geos
        wget --quiet http://download.osgeo.org/geos/geos-3.4.2.tar.bz2
        tar xfj geos-3.4.2.tar.bz2
        cd geos-3.4.2
        ./configure > /dev/null
        make -s > /dev/null
        sudo make -s install > /dev/null
        cd ..

        # gdal 1.10.x
        wget --quiet http://download.osgeo.org/gdal/1.10.1/gdal-1.10.1.tar.gz
        tar xfz gdal-1.10.1.tar.gz
        cd gdal-1.10.1
        ./configure --with-python > /dev/null
        make -s > /dev/null
        sudo make -s install > /dev/null
        cd ..

        # postgis
        wget --quiet http://download.osgeo.org/postgis/source/postgis-2.1.3.tar.gz
        tar xfz postgis-2.1.3.tar.gz
        cd postgis-2.1.3
        ./configure > /dev/null
        make -s > /dev/null
        sudo make -s install > /dev/null
        sudo ldconfig
        sudo make comments-install > /dev/null
        sudo ln -sf /usr/share/postgresql-common/pg_wrapper /usr/local/bin/shp2pgsql
        sudo ln -sf /usr/share/postgresql-common/pg_wrapper /usr/local/bin/pgsql2shp
        sudo ln -sf /usr/share/postgresql-common/pg_wrapper /usr/local/bin/raster2pgsql
    popd
fi
############################################

# for Travis CI, these things are installed in .travis.yml, to be in correct environment
if [ "$INSTALL_TYPE" != "travis" ]; then
    # Install Django
    # TODO remove this once 1.7 is released and we can install using pip.
    pushd $TEMP_ROOT
        # Check for Django version
        django_vers=`python -c "import django; print(django.get_version())"` || true
        if [ '1.7c1' != "$django_vers" ]; then
            echo "Installing Django"
            pip install -q -U git+git://github.com/django/django.git@1.7c1
        else
            echo 'Django already found, skipping.'
        fi
    popd

    pushd $PROJECT_ROOT
        pip install -q -r "deployment/requirements.txt"
    popd

    # Install node dependencies
    npm install -g grunt-cli yo generator-angular

    # Install ruby gems
    gem install -v 3.3.4 sass
    gem install -v 0.12.5 compass
fi

#########################
# Database setup        #
#########################
# Set up empty database with spatial extension
pushd $PROJECT_ROOT
    # Needs to run as postgres user, which is only possible via a separate script.
    sudo -u postgres ./deployment/setup_db.sh $DB_NAME $DB_USER $DB_PASS
popd

#########################
# RabbitMQ setup        #
#########################
pushd $PROJECT_ROOT
    sudo ./deployment/setup_rabbitmq.sh $WEB_USER $VHOST_NAME
popd

#########################
# Django setup          #
#########################
pushd $DJANGO_ROOT
    # Try to create a settings file for the specified install type
    if [ -f "transit_indicators/settings/$INSTALL_TYPE.py" ]; then
        init="transit_indicators/settings/__init__.py"
        echo '# This file generated automatically during the install process' > $init
        echo '# It will be overwritten if you re-run provision.sh' >> $init
        echo "from $INSTALL_TYPE import "'*' >> $init
    else
        echo "Couldn't find settings file for the install type $INSTALL_TYPE" >&2
        exit 1
    fi

    # Generate a unique key for each provision; don't regenerate on each new provision..
    keyfile='transit_indicators/settings/secret_key.py'
    if [ ! -f "$keyfile" ]; then
        echo '# This file created automatically during the provision process.' > $keyfile
        KEY=$(< /dev/urandom tr -dc '_!@#$%^&*(\-_=+)a-z-0-9' | head -c50;)
        echo "SECRET_KEY = '$KEY'" >> $keyfile
    fi

    # Write out database settings that match what this script is setting up.
    django_conf_file='transit_indicators/settings/provision_settings.py'
    django_conf="# Additional settings for Django; this file is created automatically
# by the provision script and will be overwritten if you re-run provision.sh.

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.postgresql_psycopg2',
        'NAME': '$DB_NAME',
        'USER': '$DB_USER',
        'PASSWORD': '$DB_PASS',
        'HOST': '$DB_HOST',
        'PORT': '$DB_PORT'
    }
}

MEDIA_ROOT = '$UPLOADS_ROOT'

# RabbitMQ settings
BROKER_URL = 'amqp://$WEB_USER:$WEB_USER@$RABBIT_MQ_HOST:$RABBIT_MQ_PORT/$VHOST_NAME'
"
    echo "$django_conf" > "$django_conf_file"

    # Create folder to hold user uploads
    if [ ! -d "$UPLOADS_ROOT" ]; then
        mkdir $UPLOADS_ROOT
        chown "$WEB_USER":"$WEB_USER" $UPLOADS_ROOT
    fi
    if [ "$INSTALL_TYPE" != "travis" ]; then
        sudo -Hu "$WEB_USER" python manage.py migrate --noinput
    fi
popd

# Add deletion trigger. This can't happen in setup_db.sh, above, because
# it relies on Django migrations.
pushd $PROJECT_ROOT
    sudo -u postgres psql -d $DB_NAME -f ./deployment/delete_gtfs_trigger.sql
popd

## Now create a superuser for the app
pushd $DJANGO_ROOT
    sudo -Hu "$WEB_USER" python manage.py oti_create_user --username="$APP_SU_USERNAME" --password="$APP_SU_PASSWORD" --email="$APP_SU_EMAIL" --superuser
popd

#########################
# Celery setup          #
#########################
echo ''
echo "Setting up celery upstart service"

celery_conf="
start on runlevel [2345]
stop on runlevel [!2345]

kill timeout 30

chdir $DJANGO_ROOT

exec /usr/local/bin/celery worker --app transit_indicators.celery_settings --logfile $LOG_ROOT/celery.log -l debug --autoreload --concurrency=3
"

celery_conf_file="/etc/init/oti-celery.conf"
echo "$celery_conf" > "$celery_conf_file"
service oti-celery restart

echo "Finished setting up celery and background process started"

#########################
# Angular setup         #
#########################
if [ "$INSTALL_TYPE" != "travis" ]; then
    pushd "$ANGULAR_ROOT"
        # Bower gets angry if you run it as root, so external script again.
        # Hu preserves home directory settings.
        sudo -Hu "$WEB_USER" $PROJECT_ROOT/deployment/setup_angular.sh "$INSTALL_TYPE"
    popd
fi

#########################
# Windshaft setup       #
#########################
# Cannot have comments or trailing commas in a json config
windshaft_conf="
{
    \"redis_host\": \"$REDIS_HOST\",
    \"redis_port\": \"$REDIS_PORT\",
    \"db_user\": \"$DB_USER\",
    \"db_pass\": \"$DB_PASS\",
    \"db_host\": \"$DB_HOST\",
    \"db_port\": \"$DB_PORT\"
}
"

pushd $WINDSHAFT_ROOT
    echo "$windshaft_conf" > settings.json
    if [ "$INSTALL_TYPE" != "travis" ]; then
        ## Run as non-sudo user so npm installs libs as not sudo
        sudo -u $WEB_USER npm install --silent  # Travis installs npm stuff in its own setup
    fi
popd

# create test table for Windshaft
# TODO:  eventually make something for real data instead
if [ "$INSTALL_TYPE" == "development" ]
then
    echo "Adding test table for Windshaft."
    pushd $PROJECT_ROOT/deployment
        sudo -u postgres psql -d $DB_NAME -f setup_windshaft_test.sql
    popd
fi

windshaft_upstart="
description 'Start the Windshaft server'

start on runlevel [2345]
stop on runlevel [!2345]

chdir $WINDSHAFT_ROOT
exec sudo -u $WEB_USER /bin/bash -c 'nodejs server.js >> $WINDSHAFT_ROOT/windshaft.log 2>&1'
"

windshaft_upstart_file="/etc/init/oti-windshaft.conf"
echo "$windshaft_upstart" > $windshaft_upstart_file
service oti-windshaft restart

echo "Finished setting up Windshaft, and service started."

###############################
# GeoTrellis local repo setup #
###############################
echo 'Setting up local geotrellis repo'
if [ ! -d "$GEOTRELLIS_REPO_ROOT" ]; then
    pushd $PROJECTS_DIR
        git clone -b $GEOTRELLIS_REPO_BRANCH $GEOTRELLIS_REPO_URI
    popd
fi
pushd $GEOTRELLIS_REPO_ROOT
    git pull
    ./sbt "project proj4" publish-local
    ./sbt "project feature" publish-local
    ./sbt "project slick" publish-local
popd

################################
# GTFS parser local repo setup #
################################
echo 'Setting up local GTFS parser repo'
if [ ! -d "$GTFS_PARSER_REPO_ROOT" ]; then
    pushd $PROJECTS_DIR
        git clone -b $GTFS_PARSER_REPO_BRANCH $GTFS_PARSER_REPO_URI
    popd
fi
pushd $GTFS_PARSER_REPO_ROOT
    git pull
    ./sbt publish-local
popd

#########################
# GeoTrellis setup      #
#########################
echo 'Setting up geotrellis'

gt_application_conf="// This file created by provision.sh, and will be overwritten if reprovisioned.
geotrellis.catalog = \"$GEOTRELLIS_CATALOG\"
geotrellis.port = \"$GEOTRELLIS_PORT\"
database.name = \"$DB_NAME\"
database.user = \"$DB_USER\"
database.password = \"$DB_PASS\"
spray.can.server.idle-timeout = 180 s
spray.can.server.request-timeout = 120 s
"

pushd $GEOTRELLIS_ROOT/src/main/resources/
    echo "$gt_application_conf" > application.conf
popd

geotrellis_conf="start on runlevel [2345]
stop on runlevel [!2345]

kill timeout 30

chdir $GEOTRELLIS_ROOT

exec ./sbt run
"
geotrellis_conf_file="/etc/init/oti-geotrellis.conf"
echo "$geotrellis_conf" > "$geotrellis_conf_file"
service oti-geotrellis restart
echo "Geotrellis service now running"

#########################
# Gunicorn setup        #
#########################
echo ''
echo 'Copying gunicorn upstart script'
gunicorn_conf="start on runlevel [2345]
stop on runlevel [!2345]

kill timeout 30

chdir $DJANGO_ROOT

exec /usr/bin/gunicorn --workers $GUNICORN_WORKERS --log-file $LOG_ROOT/gunicorn.log -b unix:/tmp/gunicorn.sock transit_indicators.wsgi:application $GUNICORN_MAX_REQUESTS
"
gunicorn_conf_file="/etc/init/oti-gunicorn.conf"
echo "$gunicorn_conf" > "$gunicorn_conf_file"
service oti-gunicorn restart
echo 'Gunicorn now running'

#########################
# Nginx setup           #
#########################
echo ''
echo 'Setting up nginx'

nginx_conf="server {
    root $ANGULAR_STATIC;
    index index.html index.htm;

    charset utf-8;

    listen 80;
    server_name _;

    gzip on;
    gzip_static on;
    gzip_vary on;
    gzip_proxied any;
    gzip_types application/x-javascript application/json text/css text/plain;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /gt {
        proxy_pass $GEOTRELLIS_HOST;
        proxy_read_timeout 300s;
        proxy_redirect off;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
    }

    location /api {
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header Host \$http_host;
        proxy_pass http://unix:/tmp/gunicorn.sock:;
        client_max_body_size 50M;
    }

    location /tiles {
        proxy_pass $WINDSHAFT_HOST;
        proxy_redirect off;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
    }

    location /static {
        alias $DJANGO_STATIC_FILES_ROOT;
    }
}
"
nginx_conf_file="/etc/nginx/sites-enabled/oti"
echo "$nginx_conf" > "$nginx_conf_file"

# check if file exists before removing it (else rm fails on re-provision)
if [ -a "/etc/nginx/sites-enabled/default" ]; then
    echo 'Removing default nginx config'
    rm /etc/nginx/sites-enabled/default
fi

echo 'Restarting nginx'
service nginx restart
echo 'Nginx now running'

# Remind user to set their timezone -- interactive, so can't be done in provisioner script
echo ''
echo 'Setup completed successfully.'
echo 'Now run `dpkg-reconfigure tzdata` to set your timezone.' >&2
