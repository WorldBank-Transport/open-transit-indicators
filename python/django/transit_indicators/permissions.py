from rest_framework.permissions import (
    SAFE_METHODS,
    BasePermission
)


class IsAuthenticatedAndAdminUserOrReadOnly(BasePermission):
    """Full permissions for admin and read-only for others, must be authenticated

    SAFE_METHODS includes (GET, HEAD, OPTIONS), see:
    https://github.com/tomchristie/django-rest-framework/blob/2.3.13/rest_framework/permissions.py
    
    """
    def has_permission(self, request, view):
        if not request.user.is_authenticated():
            return False
        is_admin = (request.user and request.user.is_staff)
        return is_admin or request.method in SAFE_METHODS
              

