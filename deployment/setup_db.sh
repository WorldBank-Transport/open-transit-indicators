#!/bin/bash
# Parameters (this is designed to be called from another script)
DB_NAME=$1
DB_USER=$2
DB_PASS=$3

# Set up the spatial template database
# Don't do anything if the database already exists and is spatial
psql -d $DB_NAME -c "SELECT PostGIS_full_version();" 1>/dev/null 2>&1
has_spatial_db=$?
if [ 0 -ne $has_spatial_db ]; then
    createdb template_postgis --encoding="UTF-8" --template=template0
    psql -d template_postgis -c "CREATE EXTENSION postgis;"
    psql -d postgres -c "UPDATE pg_database SET datistemplate='true' WHERE datname='template_postgis';"

    # Create an app db user
    createuser $DB_USER --no-superuser --no-createdb --no-createrole
    psql -d postgres -c "ALTER USER $DB_USER WITH PASSWORD '$DB_PASS';"

    # Create the app database
    createdb --owner=$DB_USER $DB_NAME --template=template_postgis
else
    echo "Spatial database already exists; skipping."
fi
