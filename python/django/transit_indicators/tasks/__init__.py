
from transit_indicators.models import IndicatorJob
from transit_indicators.tasks.calculate_indicators import run_indicator_calculation
from transit_indicators.celery_settings import app


@app.task(bind=True, max_retries=3)
def start_indicator_calculation(self, indicator_job_id, city_name):
    try:
        indicator_job = IndicatorJob.objects.get(pk=indicator_job_id)
        run_indicator_calculation(indicator_job, city_name)
    except Exception as e:
        IndicatorJob.objects.get(pk=indicator_job_id).job_status = IndicatorJob.StatusChoices.ERROR
        indicator_job.save()
        raise self.retry(exc=e)
