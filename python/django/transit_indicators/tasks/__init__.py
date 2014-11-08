from transit_indicators.models import IndicatorJob, Scenario
from transit_indicators.tasks.calculate_indicators import run_indicator_calculation
from transit_indicators.tasks.create_scenario import run_scenario_creation
from transit_indicators.tasks.delete_scenario import run_scenario_deletion
from transit_indicators.celery_settings import app

from celery.utils.log import get_task_logger
logger = get_task_logger(__name__)


@app.task(bind=True, max_retries=3)
def start_indicator_calculation(self, indicator_job_id):
    try:
        indicator_job = IndicatorJob.objects.get(pk=indicator_job_id)
        run_indicator_calculation(indicator_job)
    except Exception as e:
        logger.debug(e)
        logger.debug('Job failure for indicator: %s', indicator_job_id)
        IndicatorJob.objects.get(pk=indicator_job_id).job_status = IndicatorJob.StatusChoices.ERROR
        indicator_job.save()
        raise self.retry(exc=e)


@app.task(bind=True, max_retries=3)
def start_scenario_creation(self, scenario_id):
    try:
        scenario = Scenario.objects.get(pk=scenario_id)
        run_scenario_creation(scenario)
    except Exception as e:
        Scenario.objects.get(pk=scenario_id).job_status = Scenario.StatusChoices.ERROR
        scenario.save()
        raise self.retry(exc=e)


@app.task(bind=True, max_retries=3)
def start_scenario_deletion(self, db_name):
    try:
        run_scenario_deletion(db_name)
    except Exception as e:
        raise self.retry(exc=e)
