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
GTFS_PARSER_REPO_BRANCH="master"

# Time limit for indicator calculations to finish before retrying
INDICATOR_SOFT_TIME_LIMIT_SECONDS="10800"

UPLOADS_ROOT='/var/local/transit-indicators-uploads' # Storage for user-uploaded files
ANGULAR_ROOT="$PROJECT_ROOT/js/angular"
WINDSHAFT_ROOT="$PROJECT_ROOT/js/windshaft"
LOG_ROOT="$PROJECT_ROOT/logs"
WEB_USER='vagrant' # User under which web service runs.
WEB_PORT='8067'

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
GEOTRELLIS_MEM_MB=7168      # For the oti-geotrellis upstart job
RABBIT_MQ_HOST="127.0.0.1"
RABBIT_MQ_PORT="5672"
TRANSITFEED_VERSION=1.2.12

# TODO: Change user emails?
APP_SU_USERNAME="oti-admin"
APP_SU_PASSWORD=$APP_SU_USERNAME
APP_SU_EMAIL="$APP_SU_USERNAME@azavea.com"
APP_USERNAME="oti-user"
APP_PASSWORD=$APP_USERNAME
APP_EMAIL="$APP_USERNAME@azavea.com"

WINDSHAFT_PORT=4000
WINDSHAFT_HOST="http://localhost:$WINDSHAFT_PORT"

GUNICORN_WORKERS=3
GUNICORN_TIMEOUT=300

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
        # Should be set to $ANGULAR_ROOT/dist
        # Change once issue #161 is resolved
        ANGULAR_STATIC="$ANGULAR_ROOT/app"
        GUNICORN_MAX_REQUESTS=""
        WEB_USER='ubuntu'
        WEB_PORT='80'
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
        ruby1.9.3 rubygems \
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
        git htop multitail gettext \
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
        gunicorn \
        libgeos++-dev libpq-dev libbz2-dev proj libtool automake \
        monit

fi

#### build postgis and friends #############
# http://trac.osgeo.org/postgis/wiki/UsersWikiPostGIS21Ubuntu1204src

if type shp2pgsql 2>/dev/null; then
    echo 'PostGIS already installed; skipping.'
elif [ "$INSTALL_TYPE" == "travis" ]; then
    echo 'Installing PostGIS extensions on Travis'
    psql -U postgres -c "create extension postgis"
    psql -U postgres -c "create extension postgis_topology"
else
    echo 'Installing PostGIS and dependencies from source'
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

########## Install osm2pgsql ##########
# Necessary because only older versions of osm2pgsql
# available in ubuntu repos

if type osm2pgsql 2>/dev/null; then
    echo 'osm2pgsql already installed; skipping.'
elif [ "$INSTALL_TYPE" == "travis" ]; then
    echo 'Not installing osm2pgsql on travis; skipping.'
else
    echo 'Installing osm2pgsql from source'
    pushd $TEMP_ROOT
        # osm2pgsql
        git clone https://github.com/openstreetmap/osm2pgsql.git
        cd osm2pgsql
        git checkout 0.84.0
        ./autogen.sh
        ./configure
        sudo make
        sudo make install
    popd
fi

############################################
echo 'Installing python dependencies'
pushd $PROJECT_ROOT
    pip install -q -r "deployment/requirements.txt"
popd

# Install node dependencies
echo 'Installing node dependencies'
npm install -g grunt-cli yo generator-angular

# Install ruby gems
echo 'Installing ruby gems dependencies'
gem install -v 3.3.4 sass
gem install -v 0.12.5 compass

#########################
# Database setup        #
#########################
# Set up empty database with spatial extension
echo 'Setting up database'
pushd $PROJECT_ROOT
    # Needs to run as postgres user, which is only possible via a separate script.
    sudo -u postgres ./deployment/setup_db.sh $DB_NAME $DB_USER $DB_PASS
popd

#########################
# RabbitMQ setup        #
#########################
echo 'Setting up RabbitMQ'
pushd $PROJECT_ROOT
    sudo ./deployment/setup_rabbitmq.sh $WEB_USER $VHOST_NAME
popd

#########################
# Transitfeed setup     #
#########################
# This is installed manually due to a problem where the newest version
# isn't able to be installed via pip on Travis.
# docs here:  https://code.google.com/p/googletransitdatafeed/wiki/FeedValidator
if ! $(python -c "import transitfeed" &> /dev/null); then
    echo 'Setting up transitfeed'
    pushd $TEMP_ROOT
        wget https://googletransitdatafeed.googlecode.com/files/transitfeed-$TRANSITFEED_VERSION.tar.gz
        tar xzf transitfeed-$TRANSITFEED_VERSION.tar.gz
        pushd transitfeed-$TRANSITFEED_VERSION
            sudo python setup.py install
        popd
        rm -rf transitfeed-$TRANSITFEED_VERSION transitfeed-$TRANSITFEED_VERSION.tar.gz
    popd
fi

#########################
# Django setup          #
#########################
echo 'Setting up Django'
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
        'ENGINE': 'django.contrib.gis.db.backends.postgis',
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
        echo 'Setting up uploads root directory'
        mkdir $UPLOADS_ROOT
        chown "$WEB_USER":"$WEB_USER" $UPLOADS_ROOT
    fi

    echo 'Running migrations'
    sudo -Hu "$WEB_USER" python manage.py migrate --noinput

    echo 'Running collectstatic (needs to run as root)'
    python manage.py collectstatic --noinput

    echo 'Compiling translations'
    sudo -Hu "$WEB_USER" python manage.py compilemessages
popd

# Add triggers which rely on Django migrations (and which therefore can't happen in the
# main DB setup script).
pushd $PROJECT_ROOT
    echo 'Adding GTFS Delete PostgreSQL Trigger'
    sudo -u postgres psql -d $DB_NAME -f ./deployment/delete_gtfs_trigger.sql
    echo 'Adding Fishnet PostgreSQL Function'
    sudo -u postgres psql -d $DB_NAME -f ./deployment/fishnet_function.sql
    echo 'Adding Demographic Grid PostgreSQL Function'
    sudo -u postgres psql -d $DB_NAME -f ./deployment/grid_function.sql
    # This needs to be run as the transit_indicators user so that it has ownership
    # over the tables, otherwise changing the SRID from GeoTrellis fails.
    echo 'Adding Shapefile reprojection PostgreSQL triggers'
    PGPASSWORD=$DB_PASS psql -h $DB_HOST -U $DB_USER -d $DB_NAME -f ./deployment/setup_shapefile_reprojection.sql
popd

## Now create a superuser for the app
echo 'Creating superuser'
pushd $DJANGO_ROOT
    sudo -Hu "$WEB_USER" python manage.py oti_create_user --username="$APP_SU_USERNAME" --password="$APP_SU_PASSWORD" --email="$APP_SU_EMAIL" --superuser
    sudo -Hu "$WEB_USER" python manage.py oti_create_user --username="$APP_USERNAME" --password="$APP_PASSWORD" --email="$APP_EMAIL"
popd

#########################
# Celery setup          #
#########################
echo ''
echo "Setting up celery upstart services"

celery_datasources_conf="
start on (filesystem or (vagrant-mounted or cloud-final))
stop on runlevel [!2345]

kill timeout 30

chdir $DJANGO_ROOT

exec /usr/local/bin/celery worker --app transit_indicators.celery_settings --queue datasources --logfile $LOG_ROOT/celery.log -l debug --pidfile /var/run/celery-datasources.pid --autoreload --concurrency=3
"

celery_datasources_conf_file="/etc/init/oti-celery-datasources.conf"
echo "$celery_datasources_conf" > "$celery_datasources_conf_file"

# indicators
celery_indicators_conf="
start on (filesystem or (vagrant-mounted or cloud-final))
stop on runlevel [!2345]

kill timeout 30

chdir $DJANGO_ROOT

exec /usr/local/bin/celery worker --app transit_indicators.celery_settings --queue indicators --logfile $LOG_ROOT/celery.log -l debug --pidfile /var/run/celery-indicators.pid --autoreload --concurrency=1 --soft-time-limit $INDICATOR_SOFT_TIME_LIMIT_SECONDS
"

celery_indicators_conf_file="/etc/init/oti-celery-indicators.conf"
echo "$celery_indicators_conf" > "$celery_indicators_conf_file"

service oti-celery-datasources restart
service oti-celery-indicators restart

echo "Finished setting up celery and background processes started"

#########################
# Angular setup         #
#########################
echo 'Setting up angular'

angular_windshaft_conf_path="$ANGULAR_STATIC/scripts/windshaft-config.js"
angular_windshaft_conf="
// This file created by provision.sh, and will be overwritten if reprovisioned.
angular.module('transitIndicators').constant('windshaftConfig', {
    port: $WEB_PORT
});
"
echo "$angular_windshaft_conf" > "$angular_windshaft_conf_path"

if [ "$INSTALL_TYPE" != "travis" ]; then
    mkdir -p /home/$WEB_USER/.npm
    pushd "$ANGULAR_ROOT"
        # Hack to get get permissions set correctly for AMI generation
        sudo chown $WEB_USER:$WEB_USER /home/$WEB_USER/.npm
        sudo chmod 777 -R /home/$WEB_USER/.npm
        # Bower gets angry if you run it as root, so external script again.
        # Hu preserves home directory settings.
        sudo -Hu "$WEB_USER" $PROJECT_ROOT/deployment/setup_angular.sh "$INSTALL_TYPE"
    popd
fi

#########################
# Windshaft setup       #
#########################
# Windshaft is not installed on Travis, because
#  a) we don't need it to run tests
#  b) installing it causes dependency problems with the version of PostGIS installed on Travis
if [ "$INSTALL_TYPE" != "travis" ]; then
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
        ## Run as non-sudo user so npm installs libs as not sudo
        # Travis installs npm stuff in its own setup
        sudo -iu "$WEB_USER" bash -c "cd $WINDSHAFT_ROOT && npm install"
    popd

    windshaft_upstart="
    description 'Start the Windshaft server'

    start on (filesystem or (vagrant-mounted or cloud-final))
    stop on runlevel [!2345]

    script
        echo \$\$ > /var/run/windshaft.pid
        chdir $WINDSHAFT_ROOT
        exec sudo -u $WEB_USER /bin/bash -c 'nodejs server.js >> $WINDSHAFT_ROOT/windshaft.log 2>&1'
    end script

    pre-stop script
        rm /var/run/windshaft.pid
    end script
    "

    windshaft_upstart_file="/etc/init/oti-windshaft.conf"
    echo "$windshaft_upstart" > $windshaft_upstart_file
    service oti-windshaft restart

    echo "Finished setting up Windshaft, and service started."
fi

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
    ./sbt "project vector" publish-local
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
database.geom-name-lat-lng = \"the_geom\"
database.geom-name-utm = \"geom\"
database.name = \"$DB_NAME\"
database.user = \"$DB_USER\"
database.password = \"$DB_PASS\"
spray.can.server.idle-timeout = 1260 s
spray.can.server.request-timeout = 1200 s
"

pushd $GEOTRELLIS_ROOT/src/main/resources/
    echo "$gt_application_conf" > application.conf
popd

geotrellis_conf="start on (filesystem or (vagrant-mounted or cloud-final))
stop on runlevel [!2345]

kill timeout 30

script
    echo \$\$ > /var/run/oti-indicators.pid
    chdir $GEOTRELLIS_ROOT
    exec ./sbt -mem $GEOTRELLIS_MEM_MB -XX:-UseConcMarkSweepGC -XX:+UseGCOverheadLimit run
end script

pre-stop script
    rm /var/run/oti-indicators.pid
end script
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
gunicorn_conf="start on (filesystem or (vagrant-mounted or cloud-final))
stop on runlevel [!2345]

kill timeout 30

chdir $DJANGO_ROOT

exec /usr/bin/gunicorn --workers $GUNICORN_WORKERS --log-file $LOG_ROOT/gunicorn.log -p /var/run/gunicorn/gunicorn.pid -b unix:/tmp/gunicorn.sock transit_indicators.wsgi:application $GUNICORN_MAX_REQUESTS --timeout=$GUNICORN_TIMEOUT
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
        proxy_read_timeout 1800s;
        proxy_redirect off;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
    }

    location /api {
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header Host \$http_host;
        proxy_pass http://unix:/tmp/gunicorn.sock:;
        proxy_read_timeout 600s;
        client_max_body_size 100M;
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

    location = /monitoring {
        return 301 http://\$http_host\$request_uri/;
    }

    location /monitoring {
        rewrite ^/monitoring/(.*) /\$1 break;
        proxy_pass http://localhost:2812/;
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

#########################
# Monit setup           #
#########################
# Don't install monit on Travis; could lead to strange test behavior
if [ "$INSTALL_TYPE" != "travis" ]; then
    echo 'Setting up monit for service management'
    monit_conf='
    # This configuration generation by provision.sh and will be overwritten on re-provision.
    set httpd port 2812 and
      use address 127.0.0.1  # only accept connection from localhost
      allow oti-admin:oti-admin      # require user "admin" with password "monit"
    #################
    # Filesystem    #
    #################
    check filesystem root with path /
      # Ordinarily we would want to alert when free space is low but that
      # does not really work for this app.

    ###############
    # Services    #
    ###############
    # Generic service names so this will be usable to non-Azaveans.
    # nginx (http proxy)
    check process web-server-nginx with pidfile /var/run/nginx.pid
      start program = "/etc/init.d/nginx start"
      stop program = "/etc/init.d/nginx stop"
      if failed host localhost port 80
        protocol HTTP request "/static/nginx-check"
        then restart
      if 5 restarts within 5 cycles then timeout

    # postgresql (database)
    check process database-PostgreSQL with pidfile /var/run/postgresql/9.1-main.pid
      start program = "/etc/init.d/postgresql start"
      stop program = "/etc/init.d/postgresql stop"
      if failed unixsocket /var/run/postgresql/.s.PGSQL.5432
        protocol pgsql
        then restart
      if 5 restarts within 5 cycles then timeout

    # gunicorn (Web app)
    check process webapp-gunicorn with pidfile /var/run/gunicorn/gunicorn.pid
      start program = "/sbin/start oti-gunicorn"
      stop program = "/sbin/stop oti-gunicorn"
      if failed unixsocket /tmp/gunicorn.sock
        protocol HTTP request "/api/"
        then restart
      if 5 restarts within 5 cycles then timeout

    # windshaft (map tiles)
    check process tileserver-windshaft with pidfile /var/run/windshaft.pid
      start program = "/sbin/start oti-windshaft"
      stop program = "/sbin/stop oti-windshaft"
      if failed host localhost port 4000
        protocol HTTP request "/"
        then restart
      if 5 restarts within 5 cycles then timeout

    # celery indicators (indicator calculation task queue)
    check process indicator-queue-celery with pidfile /var/run/celery-indicators.pid
      start program = "/sbin/start oti-celery-indicators"
      stop program = "/sbin/stop oti-celery-indicators"

    # celery datasources (datasource import task queue)
    check process datasource-queue-celery with pidfile /var/run/celery-datasources.pid
      start program = "/sbin/start oti-celery-datasources"
      stop program = "/sbin/stop oti-celery-datasources"
      if cpu usage > 90% for 10 cycles then restart

    # indicators service (indicator calculation service)
    check process indicator-calc-scala with pidfile /var/run/oti-indicators.pid
      start program = "/sbin/start oti-geotrellis"
      stop program = "/sbin/stop oti-geotrellis"
      if cpu usage > 90% for 40 cycles then restart
    '
    monit_conf_file="/etc/monit/conf.d/oti"
    echo "$monit_conf" > "$monit_conf_file"
    service monit restart
    echo "Monit now running. Access service management console at:"
    echo "http://$HOST/monitoring/; user / pass: oti-admin / oti-admin"
fi

# Remind user to set their timezone -- interactive, so can't be done in provisioner script
echo ''
echo 'Setup completed successfully.'
echo "SU Username: $APP_SU_USERNAME"
echo "Username: $APP_USERNAME"
echo 'Now run `dpkg-reconfigure tzdata` to set your timezone.' >&2
