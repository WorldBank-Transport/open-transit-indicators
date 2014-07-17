from django.contrib.auth.models import (
        AbstractBaseUser,
        BaseUserManager)
from django.core import validators
from django.db import models
from django.utils import timezone

class OTIUserManager(BaseUserManager):
    """ Manager for custom OTI User functions """

    def create_user(self, username, email=None, password=None,
                    first_name='', last_name='', **extra_fields):
        """ Creates and saves a User with the given username, email, password. """
        now = timezone.now()
        if not username:
            raise ValueError('The given username must be set')
        if not email:
            raise ValueError('The given email must be set')

        email = BaseUserManager.normalize_email(email)
        user = self.model(username=username, email=email, is_staff=False,
                         is_active=True, is_superuser=False, last_login=now,
                         first_name=first_name, last_name=last_name, **extra_fields)

        user.set_password(password)
        user.save(using=self._db)
        return user

    def create_superuser(self, username, email, password, **extra_fields):
        """ Create a superuser with all permissions """
        u = self.create_user(username, email, password, **extra_fields)
        u.is_staff = True
        u.is_active = True
        u.is_superuser = True
        u.save(using=self._db)
        return u


class OTIUser(AbstractBaseUser):
    """ An Open Transit Indicators user """
   
    username = models.CharField('username', 
        max_length=30, 
        unique=True,
        help_text='Required. 30 characters or fewer. Letters, digits and @/./+/-/_ only.',
        validators=[
            validators.RegexValidator(r'^[\w.@+-]+$', 'Enter a valid username.', 'invalid')
        ]
    )
    email = models.EmailField('email', blank=True, max_length=254)
    first_name = models.CharField('first_name', max_length=30, blank=True)
    last_name = models.CharField('last_name', max_length=30, blank=True)
    is_staff = models.BooleanField('is_staff', default=False)
    is_active = models.BooleanField('is_active', default=True)
    is_superuser = models.BooleanField('is_superuser', default=False)
    date_joined = models.DateTimeField('date_joined', auto_now_add=True)
    objects = OTIUserManager()

    USERNAME_FIELD = 'username'
    REQUIRED_FIELDS = ['email']
