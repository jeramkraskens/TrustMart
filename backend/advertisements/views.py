# HTTP request handlers for promotional offers displayed in the marketplace.
# Read the permission settings, then the query filters, then the action methods to follow a request.
from django.utils import timezone
from django.db.models import Q
from rest_framework import viewsets, permissions
from .models import Advertisement
from .serializers import AdvertisementSerializer


class ReadOnlyOrAdmin(permissions.BasePermission):
    def has_permission(self, request, view):
        if request.method in permissions.SAFE_METHODS:
            return True
        return bool(request.user and request.user.is_authenticated and
                    (request.user.role == 'admin' or request.user.is_superuser))


class AdvertisementViewSet(viewsets.ModelViewSet):
    """Admin manages advertisements; anyone can view the currently active ones
    (used to render the homepage banner)."""
    serializer_class = AdvertisementSerializer
    permission_classes = [ReadOnlyOrAdmin]

    # Section: Banner visibility — public results follow active status and date limits.
    def get_queryset(self):
        qs = Advertisement.objects.all()
        user = self.request.user
        is_admin = user.is_authenticated and (user.role == 'admin' or user.is_superuser)
        if not is_admin or self.request.query_params.get('active_only') == 'true':
            now = timezone.now()
            qs = qs.filter(is_active=True)
            qs = qs.filter(Q(start_date__isnull=True) | Q(start_date__lte=now))
            qs = qs.filter(Q(end_date__isnull=True) | Q(end_date__gte=now))
        return qs
