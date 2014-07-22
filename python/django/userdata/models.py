from django.contrib.auth.models import AbstractUser

class OTIUser(AbstractUser):
    """Custom User class for OTI"""

    def is_in_group(self, group):
        #return self.groups.filter(name__in=group)
        return True
