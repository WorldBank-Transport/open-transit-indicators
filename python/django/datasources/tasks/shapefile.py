import os
import tempfile
import zipfile

from celery.utils.log import get_task_logger
from django.conf import settings
from django.contrib.gis.gdal import DataSource as GDALDataSource
from django.contrib.gis.geos import MultiPolygon, Polygon

from datasources.models import Boundary, BoundaryProblem, DataSourceProblem

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
