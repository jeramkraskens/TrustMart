# HTTP request handlers for moderator reviews of listing evidence.
# Read the permission settings, then the query filters, then the action methods to follow a request.
from django.utils import timezone
from django.db import transaction
from rest_framework import viewsets, permissions
from rest_framework.decorators import action
from rest_framework.response import Response
from .models import VerificationRequest
from .serializers import VerificationRequestSerializer
from accounts.permissions import IsModerator
from listings.models import Listing
from notifications.utils import notify, notify_staff


class VerificationRequestViewSet(viewsets.ModelViewSet):
    serializer_class = VerificationRequestSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        user = self.request.user
        qs = VerificationRequest.objects.all()
        if user.role in ('moderator', 'admin') or user.is_superuser:
            return qs
        # The double underscore follows the listing relationship to its seller when filtering records.
        return qs.filter(listing__seller=user)

    @transaction.atomic
    def perform_create(self, serializer):
        vr = serializer.save()
        vr.listing.status = Listing.Status.PENDING
        vr.listing.save()

    # Section: Approval — publish only the current pending listing review.
    @action(detail=True, methods=['post'], permission_classes=[IsModerator])
    @transaction.atomic
    def approve(self, request, pk=None):
        vr = self.get_object()
        listing = Listing.objects.select_for_update().get(pk=vr.listing_id)
        vr.refresh_from_db()
        vr.listing = listing
        if vr.status != VerificationRequest.Status.PENDING or vr.listing.status != Listing.Status.PENDING:
            return Response({'detail': 'This review is no longer pending. Open the latest verification request.'}, status=400)
        vr.status = VerificationRequest.Status.APPROVED
        vr.moderator = request.user
        vr.feedback = request.data.get('feedback', '')
        vr.reviewed_at = timezone.now()
        vr.save()
        vr.listing.status = Listing.Status.VERIFIED
        vr.listing.save()
        notify(vr.listing.seller, 'verification_approved',
               f'Your listing "{vr.listing.title}" has been verified and is now live.', link=f'/listings/{listing.pk}')
        notify_staff('verification_reviewed', f'{request.user.username} approved "{listing.title}".',
                     roles=('admin',), exclude=request.user.pk, link='/admin/verifications')
        return Response(VerificationRequestSerializer(vr).data)

    # Section: Rejection — save moderator feedback and notify the seller.
    @action(detail=True, methods=['post'], permission_classes=[IsModerator])
    @transaction.atomic
    def reject(self, request, pk=None):
        vr = self.get_object()
        listing = Listing.objects.select_for_update().get(pk=vr.listing_id)
        vr.refresh_from_db()
        vr.listing = listing
        if vr.status != VerificationRequest.Status.PENDING or vr.listing.status != Listing.Status.PENDING:
            return Response({'detail': 'This review is no longer pending. Open the latest verification request.'}, status=400)
        vr.status = VerificationRequest.Status.REJECTED
        vr.moderator = request.user
        vr.feedback = request.data.get('feedback', '')
        vr.reviewed_at = timezone.now()
        vr.save()
        vr.listing.status = Listing.Status.REJECTED
        vr.listing.save()
        notify(vr.listing.seller, 'verification_rejected',
               f'Your listing "{vr.listing.title}" was rejected: {vr.feedback}', link='/seller')
        notify_staff('verification_reviewed', f'{request.user.username} rejected "{listing.title}".',
                     roles=('admin',), exclude=request.user.pk, link='/admin/verifications')
        return Response(VerificationRequestSerializer(vr).data)
