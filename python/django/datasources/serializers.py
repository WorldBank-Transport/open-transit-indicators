import operator
import os

from rest_framework import serializers

from models import (GTFSFeed, GTFSFeedProblem, DataSourceProblem, Boundary,
                    BoundaryProblem, DemographicDataSource, DemographicDataSourceProblem,
                    DemographicDataFieldName, OSMData, OSMDataProblem, RealTime, RealTimeProblem)


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


class ValidateZipMixin(object):
    """Provides functionality to validate that a file has the .zip extension."""
    def _validate_zip_extension(self, filename):
        """Checks that filename ends in .zip."""
        name, extension = os.path.splitext(filename)
        if extension != '.zip':
            msg = "Uploaded filename must end in .zip."
            raise serializers.ValidationError(msg)

    def validate_zip_file(self, attrs, source):
        """ Basic validation to ensure the file name ends in .zip. """
        fileobj = attrs[source]
        self._validate_zip_extension(fileobj.name)  # Raises exception if validation fails.
        return attrs


class ValidateTxtMixin(object):
    """Provides functionality to validate that a file has the .txt extension."""
    def _validate_txt_extension(self, filename):
        """ Checks that filename ends in txt """
        name, extension = os.path.splitext(filename)
        if extension != '.txt':
            msg = "Uploaded filename must end in .txt"
            raise serializers.ValidationError(msg)

    def validate_txt_file(self, attrs, source):
        fileobj = attrs[source]
        self._validate_txt_extension(fileobj.name)
        # TODO: Validate column headers match stop_times.txt?
        return attrs


class GTFSFeedSerializer(serializers.ModelSerializer, DataSourceProblemCountsMixin,
                         ValidateZipMixin):
    problems = serializers.SerializerMethodField('get_datasource_problem_counts')

    def validate_source_file(self, attrs, source):
        return self.validate_zip_file(attrs, source)

    class Meta:
        model = GTFSFeed
        problem_model = GTFSFeedProblem
        read_only_fields = ('is_valid', 'is_processed')
        ordering = ('-id',)


class RealTimeSerializer(serializers.ModelSerializer, DataSourceProblemCountsMixin,
                         ValidateTxtMixin):
    problems = serializers.SerializerMethodField('get_datasource_problem_counts')

    def validate_source_file(self, attrs, source):
        return self.validate_txt_file(attrs, source)

    class Meta:
        model = RealTime
        problem_model = RealTimeProblem
        read_only_fields = ('is_valid', 'is_processed')
        ordering = ('-id',)


class OSMDataSerializer(serializers.ModelSerializer, DataSourceProblemCountsMixin,
                        ValidateZipMixin):
    problems = serializers.SerializerMethodField('get_datasource_problem_counts')

    class Meta:
        model = OSMData
        problem_model = OSMDataProblem
        read_only_fields = ('is_valid', 'is_processed', 'is_downloaded', 'source_file')
        ordering = ('-id',)


class BoundarySerializer(serializers.ModelSerializer, DataSourceProblemCountsMixin,
                         ValidateZipMixin):
    """Serializes Boundary objects."""
    problems = serializers.SerializerMethodField('get_datasource_problem_counts')

    def validate_source_file(self, attrs, source):
        return self.validate_zip_file(attrs, source)

    class Meta:
        model = Boundary
        problem_model = BoundaryProblem
        read_only_fields = ('is_valid', 'is_processed', 'geom')
        ordering = ('-id')


class DemographicDataSourceSerializer(serializers.ModelSerializer, DataSourceProblemCountsMixin,
                                      ValidateZipMixin):
    """Serializes DemographicDataSource's fields, if any."""
    fields = serializers.SerializerMethodField('get_shapefile_fields')

    def validate_source_file(self, attrs, source):
        return self.validate_zip_file(attrs, source)

    class Meta:
        model = DemographicDataSource
        problem_model = DemographicDataSourceProblem
        read_only_fields = ('is_valid', 'is_processed', 'is_loaded', 'num_features')
        ordering = ('-id')

    def get_shapefile_fields(self, obj):
        """Get the names of the fields in the associated shapefile."""
        field_names = (DemographicDataFieldName.objects.filter(datasource=obj)
                       .values_list('name', flat=True))
        return field_names
