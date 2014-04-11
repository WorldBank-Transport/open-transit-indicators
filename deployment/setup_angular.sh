#!/bin/bash
# Script to set up angular (installs dependencies). Run from main provision script
# as the web service user.
set -o errexit

bower install --config.interactive=false
npm install

INSTALL_TYPE=$1

if [ "$INSTALL_TYPE" = "production" ]
then
    grunt build
fi
