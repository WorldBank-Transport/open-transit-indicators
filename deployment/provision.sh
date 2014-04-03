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


TEMP_ROOT="/tmp"
DJANGO_ROOT="$PROJECT_ROOT/python/django"

DB_NAME="transit_indicators"
DB_PASS=$DB_NAME
DB_USER=$DB_NAME

# Set the install type
INSTALL_TYPE=$1

#########################
# Project Dependencies  #
#########################
apt-get update
# Make add-apt-repository available
apt-get -y install python-dev

add-apt-repository -y ppa:chris-lea/node.js
add-apt-repository -y ppa:ubuntugis/ppa

# Install dependencies available via apt
# Lines roughly grouped by functionality (e.g. Postgres, python, node, etc.)
apt-get update
apt-get -y install \
    git \
    python-pip \
    libxml2-dev libxslt1-dev \
    postgresql-9.1 postgresql-server-dev-9.1 postgresql-9.1-postgis \
    nodejs \
    ruby1.9.3 rubygems \
    openjdk-7-jre scala

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
            sudo python setup.py install
        popd
        rm -rf Django-1.7b1 Django-1.7b1.tar.gz
    else
        echo 'Django already found, skipping.'
    fi
popd

# Install other Python dependencies via pip
pip install -r "$PROJECT_ROOT/deployment/requirements.txt"

# Install node dependencies
npm install -g grunt-cli

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
# Django setup          #
#########################
pushd $DJANGO_ROOT
    # Try to create a settings file for the specified install type
    if [ -e "transit_indicators/settings/$INSTALL_TYPE.py" ]; then
        init="transit_indicators/settings/__init__.py"
        echo '# This file generated automatically during the install process' > $init
        echo '# It will be overwritten if you re-run provision.sh' >> $init
        echo "from $INSTALL_TYPE import "'*' >> $init
    else
        echo "Couldn't find settings file for the install type $INSTALL_TYPE" >&2
        exit 1
    fi
    python manage.py migrate --noinput
popd

# Remind user to set their timezone -- interactive, so can't be done in provisioner script
echo ''
echo 'Setup completed successfully.'
echo 'Now run `dpkg-reconfigure tzdata` to set your timezone.' >&2
