import operator
import os

from rest_framework import serializers

from models import GTFSFeed, GTFSFeedProblem, DataSourceProblem, Boundary, BoundaryProblem


def _validate_zip_extension(filename):
    """Checks that filename ends in .zip."""
    name, extension = os.path.splitext(filename)
    if extension != '.zip':
        msg = "Uploaded filename must end in .zip."
        raise serializers.ValidationError(msg)


# TODO: Refactor as an actual custom field if we start adding lots of custom
# fields to our serializers.
class DataSourceProblemCountsMixin(object):
    """Adds a 'get_datasource_problem_counts' function to a DataSource which has a problem_set.

    To use, inherit from DataSourceProblemsMixin, and then add
    'problems = serializers.SerializerMethodField('get_datasource_problem_counts')'
    to your class and a 'problem_model' field referencing the correct *Problem model
    class as part of the Meta class (similar to how a serializer is referenced to a
    particular model by setting Meta.model).
    """
    def get_datasource_problem_counts(self, obj):
        """Serializer method field to get list of ids for errors"""
        try:
            class_lower = self.Meta.problem_model.__name__.lower()
        except AttributeError:
            raise AttributeError('Serializer\'s Meta class is missing a problem_model field.')
        problem_set_getter = operator.attrgetter(class_lower + '_set')
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
        problem_model = GTFSFeedProblem
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
        problem_model = BoundaryProblem
        read_only_fields = ('is_valid', 'is_processed', 'geom')
        ordering = ('-id')

    def validate_source_file(self, attrs, source):
        """ Ensure filename ends in .zip; full validation occurs in celery."""
        fileobj = attrs[source]
        _validate_zip_extension(fileobj.name)  # Raises exception if validation fails.
        return attrs
