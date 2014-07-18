from django.db import models


class OTIIndicatorsConfig(models.Model):
    """ Global configuration for indicator calculation. """
    # Suffixes denote database units; the UI may convert to other units for
    # display to users.
    # The local poverty line (US Dollars).
    poverty_line_usd = models.FloatField()

    # Buffer distance (meters) for indicators attempting to capture a concept of
    # nearness. E.g. "Percentage of elderly people living within X meters of a
    # bus stop."
    nearby_buffer_distance_m = models.FloatField()

    # The maximum allowable commute time (seconds). Used by the job accessibility
    # indicator to generate a travelshed for access to designated job
    # locations.
    max_commute_time_s = models.IntegerField()

    # Maximum allowable walk time (seconds). Also used by the job accessibility indicator
    # when generating its travelshed.
    max_walk_time_s = models.IntegerField()


class PeakTravelPeriod(models.Model):
    """ Stores "peak" travel times as times of day. """
    # The start of this peak travel period, as a time during an unspecified
    # day.
    start_time = models.TimeField()

    # The end of this peak travel period.
    end_time = models.TimeField()
