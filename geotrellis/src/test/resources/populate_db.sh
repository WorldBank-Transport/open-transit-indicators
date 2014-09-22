#!/bin/bash
# Parameters (this is designed to be called from another script)
DB_NAME=$1
DB_USER=$2
DB_PASS=$3

# Optional path prefix. Defaults to the current directory.
PATH_PREFIX=${4:-.}

# Create the app database
createdb --owner=$DB_USER $DB_NAME --template=template_postgis

# add osm data
psql -d $DB_NAME -f $PATH_PREFIX/geotrellis/src/test/resources/philly_osm/philly_osm_data.sql

# add boundary data
psql -d $DB_NAME -f $PATH_PREFIX/geotrellis/src/test/resources/philly_bounds/philly_city_bounds.sql

