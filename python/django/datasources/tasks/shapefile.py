import os
import tempfile
import zipfile

from celery.utils.log import get_task_logger
from django.conf import settings
from django.contrib.gis.gdal import DataSource as GDALDataSource
from django.contrib.gis.geos import MultiPolygon, Polygon

from datasources.models import (Boundary, BoundaryProblem, DataSourceProblem,
                                DemographicDataSource, DemographicDataSourceProblem,
                                DemographicDataFieldName, DemographicDataFeature)

# set up shared task logger
logger = get_task_logger(__name__)


class ShapeFileErrorFactory(object):
    """Constructs *Problem instances for the Shapefile being processed."""
    def __init__(self, problemClass, problemReference, referenceField):
        """
        Params:
            :problemClass: The class of problems to be generated
            :problemReference: The object to which the problems' foreign key should point
            :referenceField: The name of the foreign key field
        """
        if not issubclass(problemClass, DataSourceProblem):
            raise ValueError("Subclass of DataSourceProblem required.")
        self.problemClass = problemClass
        self.problemReference = problemReference
        self.referenceField = referenceField

    def warn(self, title, description=''):
        """Create a warning."""
        params = dict(type=DataSourceProblem.ProblemTypes.WARNING,
                      description=description,
                      title=title)
        params[self.referenceField] = self.problemReference
        self.problemClass.objects.create(**params)

    def error(self, title, description=''):
        """Create an error."""
        params = dict(type=DataSourceProblem.ProblemTypes.ERROR,
                      description=description,
                      title=title)
        params[self.referenceField] = self.problemReference
        self.problemClass.objects.create(**params)


def extract_zip_to_temp_dir(file):
    """Extracts zipfile into a new temporary folder."""
    temp_dir = tempfile.mkdtemp()
    unzipper = zipfile.ZipFile(file)
    unzipper.extractall(temp_dir)
    unzipper.close()

    return temp_dir


def get_shapefiles_in_dir(path):
    """Finds any shapefiles in path."""
    all_files = []
    for dirpath, subdirs, files in os.walk(path):
        for name in files:
            all_files.append(os.path.join(dirpath, name))
    return filter(lambda f: f.endswith('.shp'), all_files)


def get_union(geoms):
    """Attempts to union a list of geometries."""
    if len(geoms) <= 0:
        raise ValueError('Cannot union empty feature list.')

    combined = geoms[0]
    for geom in geoms[1:]:
        combined = combined.union(geom)
    return combined


def make_multipolygon(geom):
    """Wraps Polygons in MultiPolygons"""
    if isinstance(geom.geos, Polygon):
        return MultiPolygon(geom.geos)
    elif isinstance(geom.geos, MultiPolygon):
        return geom.geos
    else:
        raise ValueError('Feature is not a MultiPolygon or Polygon')


def run_shapefile_to_boundary(boundary_id):
    """Populate a boundary's geom field from a shapefile."""
    # Get the boundary object we're processing, note that we're processing, and
    # prepare to store any errors encountered.
    boundary = Boundary.objects.get(pk=boundary_id)
    boundary.is_valid = False
    boundary.save()
    error_factory = ShapeFileErrorFactory(BoundaryProblem, boundary, 'boundary')

    def handle_error(title, description):
        """Helper method to handle shapefile errors."""
        error_factory.error(title, description)
        boundary.is_processed = True
        boundary.save()
        return

    try:
        # Set up temporary directory and unzip to there.
        temp_dir = extract_zip_to_temp_dir(boundary.source_file)
        shapefiles = get_shapefiles_in_dir(temp_dir)

        if len(shapefiles) > 1:
            handle_error('Multiple shapefiles found.', 'Upload only one shapefile at a time.')
            return
        elif len(shapefiles) < 1:
            handle_error('No shapefile found.',
                         'The zip archive must include exactly one shapefile.')
            return

        shapefile_path = os.path.join(temp_dir, shapefiles[0])
        shape_datasource = GDALDataSource(shapefile_path)
        if len(shape_datasource) > 1:
            handle_error('Multiple layers in shapefile.',
                         'The boundary shapefile must have only one layer.')
            return

        boundary_layer = shape_datasource[0]
        if boundary_layer.srs is None:
            handle_error('Missing .prj file.', 'Boundary shapefile must include a .prj file.')
            return

        # Since this will become a boundary for a city / region, attempt to flatten
        # all features into one feature.
        try:
            union = get_union([feature.geom for feature in boundary_layer])
        except ValueError as e:
            handle_error('Could not create geometry union.', str(e))
            return

        # Transform to our internal database SRID
        union.transform(settings.DJANGO_SRID)

        # Wrap in a MultiPolygon if necessary
        geometry = make_multipolygon(union)

        # Write out the data and save
        boundary.geom = geometry
        boundary.is_valid = True
        boundary.is_processed = True
        boundary.save()
    except Exception as e:
        handle_error('Unexpected error processing shapefile.', str(e))
        return


def run_get_shapefile_fields(demographicdata_id):
    """Get the column field names from a shapefile.
    Opens the Shapefile associated with demographicdata_id, validates it, and extracts the field
    names available in the Shapefile. Saves them as DemographicDataFieldName objects.
    Params:
        :demographicdata_id: ID of a DemographicDataSource from which to get field names.
    """
    # Note that we're processing, and
    # prepare to store any errors encountered.
    demog_data = DemographicDataSource.objects.get(pk=demographicdata_id)
    demog_data.is_valid = False
    demog_data.save()
    error_factory = ShapeFileErrorFactory(DemographicDataSourceProblem, demog_data, 'source_file')

    def handle_error(title, description):
        """Helper method to handle shapefile errors."""
        error_factory.error(title, description)
        demog_data.is_processed = True
        demog_data.save()
        return

    try:
        # Set up temporary directory and unzip to there.
        temp_dir = extract_zip_to_temp_dir(demog_data.source_file)
        shapefiles = get_shapefiles_in_dir(temp_dir)

        if len(shapefiles) > 1:
            handle_error('Multiple shapefiles found.', 'Upload only one shapefile at a time.')
            return
        elif len(shapefiles) < 1:
            handle_error('No shapefile found.',
                         'The zip archive must include exactly one shapefile.')
            return

        shapefile_path = os.path.join(temp_dir, shapefiles[0])
        shape_datasource = GDALDataSource(shapefile_path)
        if len(shape_datasource) > 1:
            handle_error('Multiple layers in shapefile.',
                         'The boundary shapefile must have only one layer.')
            return
        demographic_layer = shape_datasource[0]
        if demographic_layer.srs is None:
            handle_error('Missing .prj file.', 'Demographic shapefile must include a .prj file.')
            return

        for field_name in demographic_layer.fields:
            DemographicDataFieldName.objects.get_or_create(datasource=demog_data, name=field_name)

        demog_data.num_features = len(demographic_layer)
        demog_data.is_valid = True
        demog_data.is_processed = True
        demog_data.save()

    except Exception as e:
        handle_error('Unexpected error processing shapefile.', str(e))
        return


def run_load_shapefile_data(demographicdata_id, pop1_field, pop2_field, dest1_field):
    """Load data from DemographicDataSource into DemographicDataFeature objects.
    Opens the Shapefile associated with demographicdata_id and reads data from the fields
    whose names are given in pop1_field, pop2_field, and dest1_field. Generates a
    series of DemographicDataFeature objects containing the geometries in the shapefile,
    as well as the data from the three specified fields associated with each geometry.
    Params:
        :pop1_field: The name of the field in the Shapefile from which to import data that
        will be placed in the population_metric_1 field of each generated
        DemographicDataFeature object.
        :pop2_field: Same as pop1_field, but data goes into the population_metric_2 field
        of the generated DemographicDataFeature objects.
        :dest2_field: Same as pop1_field and pop2_field, but data from this field of the
        Shapefile will end up in the destination_metric_1 field of each
        DemographicDataFeature object.
    """
    # We can assume that the shapefile is valid because get_shapefile_fields
    # has been run, so jump straight to getting the data.
    demog_data = DemographicDataSource.objects.get(pk=demographicdata_id)

    # Get rid of any existing data--need to do a full reload if user changes
    # the configuration.
    # demog_data should have its is_loaded attribute set to False by the view
    # which launches this job.
    demog_data.demographicdatafeature_set.all().delete()
    error_factory = ShapeFileErrorFactory(DemographicDataSourceProblem, demog_data, 'source_file')
    try:
        temp_dir = extract_zip_to_temp_dir(demog_data.source_file)
        shapefile = os.path.join(temp_dir, get_shapefiles_in_dir(temp_dir)[0])
        data_layer = GDALDataSource(shapefile)[0]

        for feature in data_layer:
            try:
                pop1_val = feature.get(str(pop1_field)) if pop1_field else None
                pop2_val = feature.get(str(pop2_field)) if pop2_field else None
                dest1_val = feature.get(str(dest1_field)) if dest1_field else None
                # If this is too slow we can maybe speed it up with bulk_create
                (DemographicDataFeature.objects
                 .get_or_create(population_metric_1=pop1_val,
                                population_metric_2=pop2_val,
                                destination_metric_1=dest1_val,
                                geom=make_multipolygon(feature.geom.transform(settings.DJANGO_SRID,
                                                                              clone=True)),
                                datasource=demog_data))
            except ValueError as e:
                error_factory.warn('Could not import 1 feature.', str(e))

        demog_data.is_loaded = True
        demog_data.save()
        num_loaded_features = DemographicDataFeature.objects.filter(datasource=demog_data).count()
        if (num_loaded_features < demog_data.num_features):
            error_factory.warn('Only %s out of %s features loaded; some features may be invalid.'
                               % (num_loaded_features, demog_data.num_features))
    except Exception as e:
        error_factory.error('Unexpected error loading shapefile.', str(e))
        demog_data.is_loaded = True
        demog_data.is_valid = False
        demog_data.save()
        return
