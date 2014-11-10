from django.db.models.signals import post_delete
from django.dispatch import receiver

from transit_indicators.tasks import start_scenario_deletion


@receiver(post_delete, sender='transit_indicators.Scenario')
def delete_scenario_db(sender, **kwargs):
    """Delete a scenario's database when that scenario is deleted"""
    scenario = kwargs.get('instance', None)
    if scenario:
        start_scenario_deletion.apply_async(args=[scenario.db_name], queue='scenarios')
