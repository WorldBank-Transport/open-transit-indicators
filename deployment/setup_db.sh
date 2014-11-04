#!/bin/bash
# Parameters (this is designed to be called from another script)
DB_NAME=$1
DB_USER=$2
DB_PASS=$3

# Optional path prefix. Defaults to the current directory.
PATH_PREFIX=${4:-.}

# Set up the spatial template database if necessary
psql -d template_postgis -c "SELECT PostGIS_full_version();" 1>/dev/null 2>&1
has_template_postgis=$?
if [ 0 -ne $has_template_postgis ]; then
    createdb template_postgis --encoding="UTF-8" --template=template0
    psql -d template_postgis -c "CREATE EXTENSION postgis;"
    psql -d postgres -c "UPDATE pg_database SET datistemplate='true' WHERE datname='template_postgis';"
else
    echo 'Spatial template already exists; not creating it.'
fi

# Set up the database user
# Test lifted from
# http://stackoverflow.com/questions/8546759/how-to-check-if-a-postgres-user-exists
has_db_user=`psql postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname='$DB_USER'"`
if [ "1" != "$has_db_user" ]; then
    createuser $DB_USER --no-superuser --createdb --no-createrole
    psql -d postgres -c "ALTER USER $DB_USER WITH PASSWORD '$DB_PASS';"
else
    echo "Database user $DB_USER already exists; not creating it."
fi

# Don't create the database if it already exists and is spatial.
psql -d $DB_NAME -c "SELECT PostGIS_full_version();" 1>/dev/null 2>&1
has_spatial_db=$?
if [ 0 -ne $has_spatial_db ]; then
    # Create the app database
    createdb --owner=$DB_USER $DB_NAME --encoding="UTF-8" --template=template_postgis

    # Setup the GTFS tables. These are used by GeoTrellis and Windshaft and shouldn't
    # need to be accessed from within Django, which is why they are not set up as models.
    psql -d $DB_NAME -f $PATH_PREFIX/deployment/setup_gtfs.sql

    # add database triggers
    psql -d $DB_NAME -f $PATH_PREFIX/deployment/stops_routes_trigger.sql

    # Populate the UTM zone->srid lookup table
    psql -d $DB_NAME -f $PATH_PREFIX/deployment/utm_zone_boundaries.sql
else
    echo 'Spatial database already exists; skipping.'
fi

# Temporary hack to fix performance problems on GTFS imports. This trigger was the
# bottleneck as it was being run on each insert to the stops table. Removing it here
# allows GTFS imports to complete in a reasonable amount of time, but at the expense
# of not being able to view a stop's served routes in the UI. This functionality will
# be added back when we come up with a quicker calculation mechanism.
psql -d $DB_NAME -c "DROP TRIGGER IF EXISTS stops_routes ON gtfs_stops;"
