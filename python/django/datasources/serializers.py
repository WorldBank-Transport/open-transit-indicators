import operator
import os

from rest_framework import serializers

from models import GTFSFeed, DataSourceProblem, Boundary


def _validate_zip_extension(filename):
    """Checks that filename ends in .zip."""
    name, extension = os.path.splitext(filename)
    if extension != '.zip':
        msg = "Uploaded filename must end in .zip."
        raise serializers.ValidationError(msg)


class DataSourceProblemCountsMixin(object):
    """Adds a 'get_datasource_problem_counts' function to a DataSource which has a problem_set.

    This requires that the Problems class follow the following naming convention:
        The problems for a DataSource class called 'FooDataSource' should be represented by
        a class called 'FooDataSourceProblem'.

    To use, inherit from DataSourceProblemsMixin, and then add
    'problems = serializers.SerializerMethodField('get_datasource_problem_counts')'
    to your class.
    """
    def get_datasource_problem_counts(self, obj):
        """Serializer method field to get list of ids for errors"""
        class_lower = obj.__class__.__name__.lower()
        problem_set_getter = operator.attrgetter(class_lower + 'problem_set')
        errors = (problem_set_getter(obj)
                  .filter(type=DataSourceProblem.ProblemTypes.ERROR)
                  .count())
        warnings = (problem_set_getter(obj)
                    .filter(type=DataSourceProblem.ProblemTypes.WARNING)
                    .count())
        return dict(errors=errors, warnings=warnings)


class GTFSFeedSerializer(serializers.ModelSerializer, DataSourceProblemCountsMixin):
    problems = serializers.SerializerMethodField('get_datasource_problem_counts')

    class Meta:
        model = GTFSFeed
        read_only_fields = ('is_valid', 'is_processed')
        ordering = ('-id',)

    def validate_source_file(self, attrs, source):
        """ Basic validation to ensure the file name ends in .zip. """
        fileobj = attrs[source]

        _validate_zip_extension(fileobj.name)  # Raises exception if validation fails.
        return attrs


class BoundarySerializer(serializers.ModelSerializer, DataSourceProblemCountsMixin):
    """Serializes Boundary objects."""
    problems = serializers.SerializerMethodField('get_datasource_problem_counts')

    class Meta:
        model = Boundary
        read_only_fields = ('is_valid', 'is_processed', 'geom')
        ordering = ('-id')

    def validate_source_file(self, attrs, source):
        """ Ensure filename ends in .zip; full validation occurs in celery."""
        fileobj = attrs[source]
        _validate_zip_extension(fileobj.name)  # Raises exception if validation fails.
        return attrs
