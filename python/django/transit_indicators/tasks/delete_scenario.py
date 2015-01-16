from celery.utils.log import get_task_logger
from django.db import connection

logger = get_task_logger(__name__)


def run_scenario_deletion(db_name):
    """Deletes the database associated with a scenario."""
    logger.debug('Deleting database for scenario %s.', db_name)
    cursor = connection.cursor()
    # First disconnect any existing sessions
    cursor.execute("UPDATE pg_database SET datallowconn = 'false' WHERE datname = %s;", (db_name,))
    cursor.execute("SELECT pg_terminate_backend(procpid) FROM pg_stat_activity WHERE datname = %s;", (db_name,))
	# Now can drop database
    cursor.execute('DROP DATABASE IF EXISTS "%s";', (db_name,))
