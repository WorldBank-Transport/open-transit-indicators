from celery.utils.log import get_task_logger
from django.db import connection

logger = get_task_logger(__name__)


def run_scenario_deletion(db_name):
    """Deletes the database associated with a scenario."""
    logger.debug('Deleting database for scenario %s.', db_name)
    cursor = connection.cursor()
    cursor.execute('DROP DATABASE IF EXISTS "%s";', (db_name,))
