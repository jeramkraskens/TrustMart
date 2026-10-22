# HTTP request handlers for orders and simulated escrow payment states.
# Read the permission settings, then the query filters, then the action methods to follow a request.
from django.db.models import Q
from django.db import transaction
from rest_framework.exceptions import ValidationError
from rest_framework import viewsets, permissions
from rest_framework.decorators import action
from rest_framework.response import Response
from .models import Transaction
from .serializers import TransactionSerializer
from listings.models import Listing
from notifications.utils import notify, notify_staff


class TransactionViewSet(viewsets.ModelViewSet):
    serializer_class = TransactionSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        user = self.request.user
        if user.is_superuser or user.role == 'admin':
            return Transaction.objects.all()
        return Transaction.objects.filter(Q(buyer=user) | Q(listing__seller=user))

    # Section: Demo checkout — lock the listing to prevent duplicate purchases.
    @transaction.atomic
    def perform_create(self, serializer):
        listing = Listing.objects.select_for_update().get(pk=serializer.validated_data['listing'].pk)
        if listing.status != Listing.Status.VERIFIED or listing.transactions.exists():
            raise ValidationError({'detail': 'This listing is no longer available to purchase.'})
        # Use the signed-in buyer and database price, rather than trusting client-supplied ownership or amounts.
        # This saves a demo order; it does not call a payment provider.
        txn = serializer.save(buyer=self.request.user, amount=listing.price)
        listing.status = Listing.Status.SOLD
        listing.save()
        notify(listing.seller, 'new_sale', f'"{listing.title}" purchased. Demo payment held in escrow.', link='/seller')
        notify(txn.buyer, 'order_created', f'Order #{txn.pk} for "{listing.title}" was placed. Demo payment held in escrow.', link='/buyer')
        notify_staff('order_created', f'New order #{txn.pk} for "{listing.title}".', roles=('admin',), link='/admin/transactions')

    @action(detail=True, methods=['post'])
    @transaction.atomic
    def confirm_delivery(self, request, pk=None):
        """Buyer confirms delivery -> escrow released to seller."""
        txn = self.get_object()
        txn = Transaction.objects.select_for_update().get(pk=txn.pk)
        if txn.buyer != request.user:
            return Response({'detail': 'Only the buyer can confirm delivery.'}, status=403)
        if txn.delivery_confirmed:
            return Response(TransactionSerializer(txn).data)
        if txn.escrow_status != Transaction.EscrowStatus.HELD or txn.disputes.filter(status='open').exists():
            return Response({'detail': 'This order is settled or has an open dispute.'}, status=400)
        # The release below updates stored demo state; no bank transfer happens in this method.
        txn.delivery_confirmed = True
        txn.escrow_status = Transaction.EscrowStatus.RELEASED
        txn.save()
        notify(txn.listing.seller, 'payment_released',
               f'Delivery confirmed for "{txn.listing.title}". Demo escrow released.', link='/seller')
        notify(txn.buyer, 'delivery_confirmed', f'You confirmed delivery of "{txn.listing.title}".', link='/buyer')
        notify_staff('payment_released', f'Delivery confirmed and demo escrow released for order #{txn.pk}.',
                     roles=('admin',), link='/admin/transactions')
        return Response(TransactionSerializer(txn).data)
