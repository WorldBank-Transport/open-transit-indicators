from rest_framework import serializers

from datasources.models import DemographicDataFieldName
from models import OTIIndicatorsConfig, OTIDemographicConfig, PeakTravelPeriod


class PeakTravelPeriodSerializer(serializers.ModelSerializer):
    """ Serializer for PeakTravelPeriods -- Validates start_time < end_time"""
    def validate(self, attrs):
        """ Make sure that start time is before end time. """
        if attrs['start_time'] > attrs['end_time']:
            # TODO: Translation?
            raise serializers.ValidationError("Start time comes after end time.")
        return attrs

    class Meta:
        model = PeakTravelPeriod
        fields = ('start_time', 'end_time')


class OTIIndicatorsConfigSerializer(serializers.ModelSerializer):
    """ Serializer for OTIIndicatorsConfig -- Displays PeakTravelPeriods """
    def raise_if_lt_0(self, num):
        """ Raises a ValidationError if num < 0 """
        if num < 0:
            raise serializers.ValidationError("Must be >= 0")

    def validate_poverty_line_usd(self, attrs, source):
        """ Make sure poverty_line >= 0 """
        self.raise_if_lt_0(attrs[source])
        return attrs

    def validate_nearby_buffer_distance_m(self, attrs, source):
        """ Make sure buffer distance >= 0 """
        self.raise_if_lt_0(attrs[source])
        return attrs

    # The other two fields on this model are PositiveIntegerFields, so they
    # validate themselves automatically.

    class Meta:
        model = OTIIndicatorsConfig


class OTIDemographicConfigSerializer(serializers.ModelSerializer):
    """Validates that at least one demographic data metric will receive data."""
    # Override metric fields to allow strings rather than integers for foreign
    # keys
    pop_metric_1_field = serializers.CharField(max_length=10, required=False)
    pop_metric_2_field = serializers.CharField(max_length=10, required=False)
    dest_metric_1_field = serializers.CharField(max_length=10, required=False)

    def restore_object(self, attrs, instance=None):
        """Create / update an OTIDemographicConfig instance."""
        if instance is not None:
            if attrs.get('datasource', None):
                instance.datasource = attrs['datasource']

            # Update fields from field name
            for key in attrs:
                if key in ['pop_metric_1_field', 'pop_metric_2_field', 'dest_metric_1_field']:
                    setattr(instance, key, self.field_from_field_name(attrs[key],
                                                                      instance.datasource))
                elif key in ['pop_metric_1_label', 'pop_metric_2_label', 'dest_metric_2_label']:
                    setattr(instance, key, attrs[key])
            # Currently, there are no other fields in this model.
            return instance
        else:  # Still need to convert field names to DemographicDataFieldName objects
            for key in attrs:
                if key in ['pop_metric_1_field', 'pop_metric_2_field', 'dest_metric_1_field']:
                    attrs[key] = self.field_from_field_name(attrs[key], attrs['datasource'])
            return OTIDemographicConfig(**attrs)

    def field_from_field_name(self, fieldname, datasource):
        """Get a DemographicDataFieldName instance's pk from its fieldname."""
        return DemographicDataFieldName.objects.get(datasource=datasource, name=fieldname)

    def _validate_field_name_field(self, attrs, source):
        """Make sure that a field actually exists."""
        try:
            # We don't actually care about the result if this succeeds.
            DemographicDataFieldName.objects.get(datasource=attrs.get('datasource'),
                                                 name=attrs[source])
        except DemographicDataFieldName.DoesNotExist:
            raise serializers.ValidationError('\'%s\' is not a valid field for this data source.'
                                              % attrs[source])
        return True

    def validate_pop_metric_1_field(self, attrs, source):
        self._validate_field_name_field(attrs, source)
        return attrs

    def validate_pop_metric_2_field(self, attrs, source):
        self._validate_field_name_field(attrs, source)
        return attrs

    def validate_dest_metric_1_field(self, attrs, source):
        self._validate_field_name_field(attrs, source)
        return attrs

    def validate(self, attrs):
        """Make sure at least one of pop1, pop2, and dest1 has a column name."""
        # Placeholder variables that are None if the dictionary key doesn't
        # exist so the boolean expression is shorter.
        pop1_field = attrs.get('pop_metric_1_field', None)
        pop2_field = attrs.get('pop_metric_2_field', None)
        dest1_field = attrs.get('pop_metric_1_field', None)
        if not (pop1_field or pop2_field or dest1_field):
            raise serializers.ValidationError('Must specify at least one column to load')
        return attrs

    class Meta:
        model = OTIDemographicConfig
