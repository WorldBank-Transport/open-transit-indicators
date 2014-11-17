from rest_framework import serializers

from datasources.models import DemographicDataFieldName
from transit_indicators.models import (OTIIndicatorsConfig, OTIDemographicConfig, SamplePeriod,
                                       Indicator, IndicatorJob, Scenario, OTICityName)


class SamplePeriodSerializer(serializers.ModelSerializer):
    """Serializer for SamplePeriods -- performs validation of times"""
    def validate(self, attrs):
        """Validate sample period"""
        # TODO: Error messages need to be translated

        start = attrs['period_start']
        end = attrs['period_end']

        # Start time must be before end time
        if start >= end:
            raise serializers.ValidationError("Period start comes after period end.")

        # Period must be less than 24 hours
        seconds_per_day = 60 * 60 * 24
        if (end - start).total_seconds() / seconds_per_day >= 1:
            raise serializers.ValidationError("Period must be less than 24 hours.")

        return attrs

    class Meta:
        model = SamplePeriod
        fields = ('period_start', 'period_end', 'type')


class IndicatorJobSerializer(serializers.ModelSerializer):
    """Serializer for Indicator Jobs"""

    # Fields needs to not be required to allow setting default values
    job_status = serializers.ChoiceField(choices=IndicatorJob.StatusChoices.CHOICES,
                                         required=False)
    calculation_status = serializers.CharField(required=False)

    def validate(self, attrs):
        """Handle validation to set read-only fields"""
        if not attrs.get("job_status"):
            attrs["job_status"] = IndicatorJob.StatusChoices.QUEUED

        if not attrs.get("created_by"):
            attrs["created_by"] = self.context["request"].user

        return super(IndicatorJobSerializer, self).validate(attrs)

    class Meta:
        model = IndicatorJob
        read_only_fields = ('id', 'created_by')


class ScenarioSerializer(serializers.ModelSerializer):
    """Serializer for Scenarios"""

    job_status = serializers.ChoiceField(choices=Scenario.StatusChoices.CHOICES,
                                         required=False)
    created_by = serializers.SlugRelatedField(slug_field="username", read_only=True)
    sample_period = serializers.SlugRelatedField(slug_field='type')

    def validate(self, attrs):
        """Handle validation to set read-only fields"""

        if not attrs.get('job_status'):
            attrs['job_status'] = IndicatorJob.StatusChoices.QUEUED

        if not attrs.get('created_by'):
            attrs['created_by'] = self.context['request'].user

        if ('sample_period' in attrs and
            attrs.get('sample_period').type == SamplePeriod.SamplePeriodTypes.ALLTIME):
            raise serializers.ValidationError('Scenario for alltime not allowed')

        return super(ScenarioSerializer, self).validate(attrs)

    class Meta:
        model = Scenario
        read_only_fields = ('id', 'db_name')


class IndicatorSerializer(serializers.ModelSerializer):
    """Serializer for Indicator"""

    sample_period = serializers.SlugRelatedField(slug_field='type')
    calculation_job = serializers.SlugRelatedField(slug_field='id')
    city_name = serializers.SerializerMethodField('get_city_name')
    formatted_value = serializers.SerializerMethodField('get_formatted_value')

    def get_formatted_value(self, obj):
        """Display value for units"""
        units = Indicator.IndicatorTypes.INDICATOR_UNITS.get(obj.type, None) if obj.type else None
        if obj.type == Indicator.IndicatorTypes.LINE_NETWORK_DENSITY:
            LINE_NETWORK_DENSITY_MULTIPLIER = 1000000
            return u"%s" % round(obj.value * LINE_NETWORK_DENSITY_MULTIPLIER, 2)
        elif units:
            return u"%s %s" % (round(obj.value, 2), units)
        return u"%s" % round(obj.value, 2)

    def get_city_name(self, obj):
        return obj.calculation_job.city_name

    def validate(self, attrs):
        """Validate indicator fields"""
        # TODO: Error messages need to be translated

        # Make sure empty route_ids and route_types are signified by
        # a null instead of an empty string. Without this check, the
        # endpoint will return a 500 error when an empty string is used.
        for attr in ['route_id', 'route_type']:
            if attr in attrs and attrs[attr] == '':
                attrs[attr] = None

        # Route aggregation type requires a route id and no route type
        if attrs['aggregation'] == Indicator.AggregationTypes.ROUTE:
            if 'route_id' not in attrs or not attrs['route_id']:
                raise serializers.ValidationError('Route aggregation requires route_id')
            if 'route_type' in attrs and attrs['route_type']:
                raise serializers.ValidationError('Route aggregation should not have route_type')

        # Mode aggregation type requires a route type and no route id
        if attrs['aggregation'] == Indicator.AggregationTypes.MODE:
            if 'route_id' in attrs and attrs['route_id']:
                raise serializers.ValidationError('Mode aggregation should not have route_id')
            if 'route_type' not in attrs:
                raise serializers.ValidationError('Mode aggregation requires route_type')

        return attrs

    class Meta:
        model = Indicator
        fields = ('id', 'sample_period', 'type', 'aggregation', 'route_id', 'route_type',
                  'city_bounded', 'value', 'calculation_job', 'city_name', 'the_geom', 'formatted_value')
        read_only_fields = ('id',)
        write_only_fields = ('the_geom',)


class OTIIndicatorsConfigSerializer(serializers.ModelSerializer):
    """Serializer for OTIIndicatorsConfig -- Displays indicator configurations"""
    def raise_if_lt_0(self, num):
        """ Raises a ValidationError if num < 0 """
        if num < 0:
            raise serializers.ValidationError("Must be >= 0")
    
    def validate_arrive_by_time_s(self, attrs, source):
        """ Make sure that the arrive by time is non-negative and below 24 hours"""
        seconds_in_day = 60 * 60 * 24
        num = attrs[source]
        self.raise_if_lt_0(num)
        if num >= seconds_in_day:
            raise serializers.ValidationError("Must be < %s" % seconds_in_day)
        return attrs

    def validate_max_commute_time_s(self, attrs, source):
        """ Make sure that duration time is non-negative."""
        self.raise_if_lt_0(attrs[source])
        return attrs

    def validate_poverty_line(self, attrs, source):
        """ Make sure poverty_line >= 0 """
        self.raise_if_lt_0(attrs[source])
        return attrs

    def validate_nearby_buffer_distance_m(self, attrs, source):
        """ Make sure buffer distance >= 0 """
        self.raise_if_lt_0(attrs[source])
        return attrs

    def validate_avg_fare(self, attrs, source):
        """ Make sure avg fare >= 0 """
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
        # If attrs[source] does not exist, skip validation
        # This allows us to POST only a single attr and not have the endpoint fail with an error
        except KeyError:
            return True
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


class OTICityNameSerializer(serializers.ModelSerializer):
    class Meta:
        model = OTICityName
