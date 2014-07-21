from rest_framework import serializers

from models import OTIIndicatorsConfig, PeakTravelPeriod


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
