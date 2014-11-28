from rest_framework.permissions import (
    SAFE_METHODS,
    BasePermission
)

from models import IndicatorJob

class IsAdminOrScenario(BasePermission):
    """Permissions for indicators and indicator jobs.  

    Grant full permissions to admin, and allow others to run calculations on scenarios.
    """
    def has_permission(self, request, view, obj=None):
        if not request.user.is_authenticated():
            # cannot do anything without logging in first
            return False

        if (request.user and request.user.is_staff):
            # admin user can do anything
            return True

        model_cls = getattr(view, 'model', None)
        if not obj and request.method == 'POST':
            if model_cls == IndicatorJob:
                if request.POST.get('scenario'):
                    # allow non-admin users to run new indicator jobs on scenarios
                    return True
                elif not request.POST:
                    # DRF sends an empty POST to figure out whether or not to display the POST form in the browseable API.
                    # Go ahead and show it.
                    return True
                else:
                    # non-admin user attempting to run indicators on non-scenario; deny
                    return False

        # allow anything else safe or on an object owned by the user
        return request.method in SAFE_METHODS or (obj and obj.created_by == request.user.id)


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