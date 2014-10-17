import traceback
from transit_indicators.models import IndicatorJob, Scenario
from transit_indicators.tasks.calculate_indicators import run_indicator_calculation
from transit_indicators.tasks.create_scenario import run_scenario_creation
from transit_indicators.celery_settings import app


@app.task(bind=True, max_retries=3)
def start_indicator_calculation(self, indicator_job_id):
    try:
        indicator_job = IndicatorJob.objects.get(pk=indicator_job_id)
        run_indicator_calculation(indicator_job)
    except Exception as e:
        IndicatorJob.objects.get(pk=indicator_job_id).job_status = IndicatorJob.StatusChoices.ERROR
        indicator_job.save()
        raise self.retry(exc=e)

@app.task(bind=True, max_retries=3)
def start_scenario_creation(self, scenario_id):
    print "IN START_SCENARIO_CREATION"
    try:
        scenario = Scenario.objects.get(pk=scenario_id)
        run_scenario_creation(scenario)
    except Exception as e:
        print traceback.format_exc()
        Scenario.objects.get(pk=scenario_id).job_status = Scenario.StatusChoices.ERROR
        scenario.save()
        raise self.retry(exc=e)
