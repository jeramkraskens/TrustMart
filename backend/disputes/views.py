# HTTP request handlers for buyer complaints and moderator decisions about orders.
# Read the permission settings, then the query filters, then the action methods to follow a request.
from django.db import transaction
from django.db.models import Q
from django.utils import timezone
from rest_framework import viewsets, permissions
from rest_framework.decorators import action
from rest_framework.response import Response
from .models import Dispute
from .serializers import DisputeSerializer
from accounts.permissions import IsModerator
from transactions.models import Transaction
from notifications.utils import notify, notify_staff


class DisputeViewSet(viewsets.ModelViewSet):
    serializer_class = DisputeSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        user = self.request.user
        if user.role in ('moderator', 'admin') or user.is_superuser:
            return Dispute.objects.all()
        return Dispute.objects.filter(
            Q(raised_by=user) | Q(transaction__buyer=user) | Q(transaction__listing__seller=user)
        )

    @transaction.atomic
    def perform_create(self, serializer):
        dispute = serializer.save(raised_by=self.request.user)
        txn = dispute.transaction
        for user in (txn.buyer, txn.listing.seller):
            notify(user, 'dispute_opened', f'Dispute #{dispute.pk} opened for "{txn.listing.title}".',
                   link='/seller' if user.pk == txn.listing.seller_id else '/buyer')
        notify_staff('dispute_opened', f'Dispute #{dispute.pk} needs review: "{txn.listing.title}".', link='/moderator/disputes')

    # Section: Dispute resolution — update the case and demo escrow outcome.
    @action(detail=True, methods=['post'], permission_classes=[IsModerator])
    @transaction.atomic
    def resolve(self, request, pk=None):
        dispute = self.get_object()
        # Serialize decisions on the same transaction, including separate disputes.
        txn = Transaction.objects.select_for_update().get(pk=dispute.transaction_id)
        dispute = Dispute.objects.select_for_update().get(pk=dispute.pk)
        if dispute.status != Dispute.Status.OPEN:
            return Response({'detail': 'This dispute has already been resolved.'}, status=400)
        if txn.escrow_status != Transaction.EscrowStatus.HELD:
            return Response({'detail': 'This transaction has already been settled.'}, status=400)
        resolution = request.data.get('resolution')
        notes = request.data.get('resolution_notes', '')
        if resolution not in ['buyer_favor', 'seller_favor']:
            return Response({'detail': 'resolution must be buyer_favor or seller_favor'}, status=400)

        dispute.status = Dispute.Status.RESOLVED
        dispute.resolution = resolution
        dispute.resolution_notes = notes
        dispute.moderator = request.user
        dispute.resolved_at = timezone.now()
        dispute.save()

        if resolution == 'buyer_favor':
            txn.escrow_status = Transaction.EscrowStatus.REFUNDED
            notify(txn.buyer, 'dispute_resolved', f'Dispute resolved in your favor. Demo escrow marked refunded for "{txn.listing.title}".', link='/buyer')
            notify(txn.listing.seller, 'dispute_resolved', f'Dispute for "{txn.listing.title}" resolved in the buyer\'s favor. {notes}', link='/seller')
        else:
            txn.escrow_status = Transaction.EscrowStatus.RELEASED
            notify(txn.listing.seller, 'dispute_resolved', f'Dispute resolved in your favor for "{txn.listing.title}".', link='/seller')
            notify(txn.buyer, 'dispute_resolved', f'Dispute for "{txn.listing.title}" resolved in the seller\'s favor. {notes}', link='/buyer')
        txn.save()
        notify(request.user, 'dispute_reviewed', f'You resolved dispute #{dispute.pk}: {resolution.replace("_", " ")}.', link='/moderator/disputes')
        notify_staff('dispute_reviewed', f'{request.user.username} resolved dispute #{dispute.pk}.',
                     roles=('admin',), exclude=request.user.pk, link='/admin/disputes')
        return Response(DisputeSerializer(dispute).data)
