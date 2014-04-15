import os

from rest_framework import serializers

from models import GTFSFeed, GTFSFeedProblem


class GTFSFeedSerializer(serializers.ModelSerializer):
    problems = serializers.SerializerMethodField('get_gtfs_problems')

    class Meta:
        model = GTFSFeed
        read_only_fields = ('is_valid', 'is_processed')

    def validate_source_file(self, attrs, source):
        """ Basic validation to ensure the file name ends in .zip. """
        fileobj = attrs[source]

        name, extension = os.path.splitext(fileobj.name)
        if extension != '.zip':
            msg = "Uploaded filename must end in .zip."
            raise serializers.ValidationError(msg)
        return attrs

    def get_gtfs_problems(self, obj):
        """Serializer method field to get list of ids for errors"""
        errors = (obj.gtfsfeedproblem_set
                  .filter(type=GTFSFeedProblem.ProblemTypes.ERROR)
                  .count())
        warnings = (obj.gtfsfeedproblem_set
                    .filter(type=GTFSFeedProblem.ProblemTypes.WARNING)
                    .count())
        return dict(errors=errors, warnings=warnings)
