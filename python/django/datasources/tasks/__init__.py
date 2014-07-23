"""Thin wrappers for celery tasks so that autodiscovery works without having a giant file."""
from datasources.tasks.shapefile import (run_shapefile_to_boundary, run_get_shapefile_fields,
                                         run_load_shapefile_data)
from datasources.tasks.gtfs import run_validate_gtfs
from transit_indicators.celery_settings import app


@app.task
def validate_gtfs(gtfsfeed_id):
    run_validate_gtfs(gtfsfeed_id)


@app.task
def shapefile_to_boundary(boundary_id):
    run_shapefile_to_boundary(boundary_id)


@app.task
def get_shapefile_fields(demographicdata_id):
    run_get_shapefile_fields(demographicdata_id)


@app.task
def load_shapefile_data(demographicdata_id, pop1_field, pop2_field, dest1_field):
    run_load_shapefile_data(demographicdata_id, pop1_field, pop2_field, dest1_field)
