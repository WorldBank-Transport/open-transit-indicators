#!/bin/bash
# Script to set up angular (installs dependencies). Run from main provision script
# as the web service user.
set -o errexit

bower install --config.interactive=false
npm install
