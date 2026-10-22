# Reusable access rules used by API views.
# These server checks enforce roles and ownership even if a client bypasses its screen controls.
from rest_framework.permissions import BasePermission


class IsModerator(BasePermission):
    def has_permission(self, request, view):
        return bool(request.user and request.user.is_authenticated and
                    (request.user.role == 'moderator' or request.user.is_superuser))


class IsAdmin(BasePermission):
    def has_permission(self, request, view):
        return bool(request.user and request.user.is_authenticated and
                    (request.user.role == 'admin' or request.user.is_superuser))


class IsOwnerOrReadOnly(BasePermission):
    """Object-level permission: the owner can edit; admins can edit/delete anything."""

    def has_object_permission(self, request, view, obj):
        if request.method in ('GET', 'HEAD', 'OPTIONS'):
            return True
        user = request.user
        if user.is_authenticated and (user.role == 'admin' or user.is_superuser):
            return True
        owner_field = getattr(view, 'owner_field', 'seller')
        return getattr(obj, owner_field, None) == user
