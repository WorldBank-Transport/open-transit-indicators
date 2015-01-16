from django.db.models.signals import post_delete
from django.dispatch import receiver

from transit_indicators.tasks import start_scenario_deletion, start_raster_deletion

from celery.utils.log import get_task_logger
logger = get_task_logger(__name__)

@receiver(post_delete, sender='transit_indicators.Scenario')
def delete_scenario_db(sender, **kwargs):
    """Delete a scenario's database when that scenario is deleted"""
    scenario = kwargs.get('instance', None)
    if scenario:
        start_scenario_deletion.apply_async(args=[scenario.db_name], queue='scenarios')


@receiver(post_delete, sender='transit_indicators.IndicatorJob')
def delete_raster_cache(sender, **kwargs):
    """Delete chached jobs accessibility rasters for when their indicator job is deleted"""
    indicator_job = kwargs.get('instance', None)
    if indicator_job:
        start_raster_deletion.apply_async(args=[indicator_job.id], queue='indicators')
    else:
        logger.error('Indicator job not found in delete_raster_cache!')
