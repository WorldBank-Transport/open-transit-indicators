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
    class Meta:
        model = OTIIndicatorsConfig
