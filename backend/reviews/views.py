# HTTP request handlers for ratings and feedback associated with purchases.
# Read the permission settings, then the query filters, then the action methods to follow a request.
from django.db.models import Avg
from rest_framework import permissions, viewsets
from .models import Review
from .serializers import ReviewSerializer
from notifications.utils import notify
from django.db import transaction


class IsReviewerOrAdminOrReadOnly(permissions.BasePermission):
    """Only the review's author can edit it; only the author or an admin can delete it."""

    def has_permission(self, request, view):
        if request.method in permissions.SAFE_METHODS:
            return True
        return bool(request.user and request.user.is_authenticated)

    def has_object_permission(self, request, view, obj):
        if request.method in permissions.SAFE_METHODS:
            return True
        user = request.user
        is_admin = user.role == 'admin' or user.is_superuser
        if request.method == 'DELETE':
            return obj.reviewer == user or is_admin
        return obj.reviewer == user


class ReviewViewSet(viewsets.ModelViewSet):
    serializer_class = ReviewSerializer
    permission_classes = [IsReviewerOrAdminOrReadOnly]
    queryset = Review.objects.all()

    def get_queryset(self):
        qs = Review.objects.all()
        reviewee = self.request.query_params.get('reviewee')
        if reviewee:
            qs = qs.filter(reviewee_id=reviewee)
        return qs

    @transaction.atomic
    def perform_create(self, serializer):
        review = serializer.save(reviewer=self.request.user)
        self._recalculate_trust_score(review.reviewee)
        notify(review.reviewee, 'new_review', f'{review.reviewer.username} left you a {review.rating}-star review.', link='/profile')

    def perform_destroy(self, instance):
        reviewee = instance.reviewee
        instance.delete()
        self._recalculate_trust_score(reviewee)

    # Section: Trust score — recalculate the seller score after review changes.
    def _recalculate_trust_score(self, user):
        avg = Review.objects.filter(reviewee=user).aggregate(avg=Avg('rating'))['avg']
        user.trust_score = int(60 + avg * 8) if avg else 100
        user.save()
