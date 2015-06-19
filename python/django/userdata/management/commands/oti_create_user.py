"""
Management command to create an OTIUser
"""

from datetime import datetime
import getpass
import logging
import pytz
from optparse import make_option

from django.core.management.base import BaseCommand, CommandError

from userdata.models import OTIUser


logging.basicConfig(format='%(asctime)s, %(name)s [%(levelname)s] %(message)s',
                    level=logging.DEBUG)
LOGGER = logging.getLogger(__name__)


class Command(BaseCommand):
    """Command to add a user to the OTI user list

    When run with no arguments, will prompt for required info

    """
    args = None
    help = 'Adds a new OTIUser to the database'
    option_list = BaseCommand.option_list + (
        make_option('--username', default=None, help='Username to create. Prompts if none provided.'),
        make_option('--email', default=None, help='Email for username. Prompts if none provided.'),
        make_option('--password', default=None, help='Password for username. Prompts if none provided.'),
        make_option('--superuser', action='store_true', default=False, dest='superuser',
                    help='Should the created user be a superuser?'),
    )

    def handle(self, *args, **options):
        """Method that handles creation of oti user"""

        username = options['username']
        password = options['password']
        email = options['email']
        is_superuser = options['superuser']

        # Ensure a passed username doesn't already exist
        users = OTIUser.objects.filter(username=username)
        if users.count() > 0:
            print "Username taken. Exiting."
            return

        while username is None:
            print 'Please enter a username'
            username = raw_input('Username: ')
            users = OTIUser.objects.filter(username=username)
            if len(users) > 0:
                print 'Username taken...try again.'
                username = None

        if not email:
            email = raw_input('Please provide an email for this user: ')

        while not password:
            # Get password twice to verify
            user_pw = getpass.getpass('Enter a password: ')
            user_2nd_pass = getpass.getpass('Confirm your password: ')
            if user_pw == user_2nd_pass:
                password = user_pw
            else:
                print 'Your passwords did not match. Try again.'


        user = None
        if is_superuser:
            user = OTIUser.objects.create_superuser(username, email, password)
        else:
            user = OTIUser.objects.create_user(username,
                                               email=email,
                                               password=password)

        # set last login time to something long ago
        # (default in django 1.8 is None; we're on 1.7 with non-null constraint)
        user.last_login = pytz.utc.localize(datetime(1984, 01, 01))
        user.save()

        log_message = 'Created user (%s)' % (username)
        LOGGER.info(log_message)
