#!/bin/bash
# Set the path to the project directory; all other paths will be relative to this.
PROJECT_ROOT="/projects/open-transit-indicators"

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

#########################################
# Installation configuration parameters #
#########################################
TEMP_ROOT='/tmp'
DJANGO_ROOT="$PROJECT_ROOT/python/django"
UPLOADS_ROOT='/var/local/transit-indicators-uploads' # Storage for user-uploaded files
ANGULAR_ROOT="$PROJECT_ROOT/js/angular"
WINDSHAFT_ROOT='/var/local/Windshaft'  # Going to check out 'Windshaft' repo into /var/local
WEB_USER='vagrant' # User under which web service runs.

DB_NAME="transit_indicators"
DB_PASS=$DB_NAME
DB_USER=$DB_NAME
VHOST_NAME=$DB_NAME

# Set the install type. Should be one of [development|production|jenkins].
INSTALL_TYPE=$1
case "$INSTALL_TYPE" in 
    "development")
        echo "Selecting development installation"
        WEB_USER='vagrant' # User under which web service runs.
        ;;
    "production")
        echo "Selecting production installation"
        # TODO: Set variables for production deployment here
        ;;
    "jenkins")
        echo "Selecting CI installation"
        # TODO: Set variables for jenkins deployment here
        ;;
    *)
        echo "Invalid installation type; should be one of development / production / jenkins" >&2
        exit 1
        ;;
esac


#########################
# Project Dependencies  #
#########################
apt-get update
# Make add-apt-repository available
apt-get -y install python-dev

add-apt-repository -y ppa:chris-lea/node.js
add-apt-repository -y ppa:ubuntugis/ppa
add-apt-repository -y "deb http://www.rabbitmq.com/debian/ testing main"
add-apt-repository -y ppa:mapnik/v2.2.0

# add public key for RabbitMQ
pushd $TEMP_ROOT
    wget http://www.rabbitmq.com/rabbitmq-signing-key-public.asc
    apt-key add rabbitmq-signing-key-public.asc
    rm rabbitmq-signing-key-public.asc
popd

# Install dependencies available via apt
# Lines roughly grouped by functionality (e.g. Postgres, python, node, etc.)
apt-get update
apt-get -y upgrade
apt-get -y install \
    git \
    python-pip \
    libxml2-dev libxslt1-dev \
    postgresql-9.1 postgresql-server-dev-9.1 postgresql-9.1-postgis \
    nodejs \
    ruby1.9.3 rubygems \
    openjdk-7-jre scala \
    rabbitmq-server \
    libmapnik libmapnik-dev python-mapnik mapnik-utils redis-server

# Install Django
# TODO remove this once 1.7 is released and we can install using pip.
pushd $TEMP_ROOT
    # Check for Django version
    django_vers=`python -c "import django; print(django.get_version())"` || true
    if [ '1.7b1' != "$django_vers" ]; then
        echo "Installing Django"
        pip uninstall -y Django || true
        wget https://www.djangoproject.com/m/releases/1.7/Django-1.7b1.tar.gz
        tar xzvf Django-1.7b1.tar.gz
        pushd Django-1.7b1
            python setup.py install
        popd
        rm -rf Django-1.7b1 Django-1.7b1.tar.gz
    else
        echo 'Django already found, skipping.'
    fi
popd

# Install other Python dependencies via pip
pip install -r "$PROJECT_ROOT/deployment/requirements.txt"

# Install node dependencies
npm install -g grunt-cli yo generator-angular

# Install ruby gems
gem install -v 3.3.4 sass

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
        'HOST': '127.0.0.1',
        'PORT': '5432'
    }
}

MEDIA_ROOT = '$UPLOADS_ROOT'

# RabbitMQ settings
BROKER_URL = 'amqp://$WEB_USER:$WEB_USER@127.0.0.1:5672/$VHOST_NAME'
"
    echo "$django_conf" > "$django_conf_file"

    # Create folder to hold user uploads
    if [ ! -d "$UPLOADS_ROOT" ]; then
        mkdir $UPLOADS_ROOT
        chown "$WEB_USER":"$WEB_USER" $UPLOADS_ROOT
    fi
    python manage.py migrate --noinput
popd

#########################
# Feed validator setup  #
#########################
# install Google's transit feed validator
# docs here:  https://code.google.com/p/googletransitdatafeed/wiki/FeedValidator
pushd $TEMP_ROOT
    wget https://googletransitdatafeed.googlecode.com/files/transitfeed-1.2.12.tar.gz
    tar xzf transitfeed-1.2.12.tar.gz
    pushd transitfeed-1.2.12
        sudo python setup.py install
    popd
    rm -rf transitfeed-1.2.12 transitfeed-1.2.12.tar.gz

#########################
# Angular setup         #
#########################
pushd "$ANGULAR_ROOT"
    # Bower gets angry if you run it as root, so external script again.
    # Hu preserves home directory settings.
    sudo -Hu "$WEB_USER" $PROJECT_ROOT/deployment/setup_angular.sh
popd

#########################
# Windshaft setup       #
#########################
windshaft_conf="// This file is created automatically by the provision script and will be overwritten 
// if you re-run provision.sh.
// Note, currently to run this server your table must have a column called 
// the_geom_webmercator with SRID of 3857.

var Windshaft = require('./lib/windshaft');
var _         = require('underscore');
var config = {
    base_url: '/database/:dbname/table/:table',
    base_url_notable: '/database/:dbname',
    grainstore: {
                 datasource: {
                    user:'$DB_USER',
                    password:'$DB_PASS',
                    host: '127.0.0.1',
                    port: 5432
                 }
    }, //see grainstore npm for other options
    redis: {host: '127.0.0.1', port: 6379},
    enable_cors: true,
    req2params: function(req, callback){

        // no default interactivity. to enable specify the database column you'd like to interact with
        req.params.interactivity = null;

        // this is in case you want to test sql parameters eg ...png?sql=select * from my_table limit 10
        req.params =  _.extend({}, req.params);
        _.extend(req.params, req.query);

        // send the finished req object on
        callback(null,req);
    }
};

// Initialize tile server on port 4000
var ws = new Windshaft.Server(config);
ws.listen(4000);

console.log('map tiles are now being served out of: http://127.0.0.1:4000' + config.base_url + '/:z/:x/:y');"

pushd /var/local
    rm -rf Windshaft
    git clone https://github.com/CartoDB/Windshaft.git
    pushd Windshaft
        git checkout 0de3b48cbd96f4949baa59ced8a75a327398d77a  # last known passing build
        npm install
        # create server script; run Windshaft with 'node server.js'
        echo "$windshaft_conf" > ./server.js
    popd
popd

# create test table for Windshaft
pushd $PROJECT_ROOT
    sudo -u postgres psql -d $DB_NAME -f setup_windshaft_test.sql
popd

# Remind user to set their timezone -- interactive, so can't be done in provisioner script
echo ''
echo 'Setup completed successfully.'
echo 'Now run `dpkg-reconfigure tzdata` to set your timezone.' >&2
