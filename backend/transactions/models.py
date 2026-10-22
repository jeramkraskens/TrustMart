# Database structure for orders and simulated escrow payment states.
# Django model fields describe stored values; relationships connect records in other apps.
from decimal import Decimal

from django.conf import settings
from django.db import models
from listings.models import Listing

PLATFORM_COMMISSION_RATE = Decimal('0.05')  # 5% platform commission on each completed sale


class Transaction(models.Model):
    class EscrowStatus(models.TextChoices):
        HELD = 'held', 'Held in Escrow'
        RELEASED = 'released', 'Released to Seller'
        REFUNDED = 'refunded', 'Refunded to Buyer'

    class PaymentMethod(models.TextChoices):
        CARD = 'card', 'Card (Stripe)'
        PAYHERE = 'payhere', 'PayHere'

    buyer = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='purchases')
    listing = models.ForeignKey(Listing, on_delete=models.CASCADE, related_name='transactions')
    amount = models.DecimalField(max_digits=12, decimal_places=2)
    commission_rate = models.DecimalField(max_digits=4, decimal_places=3, default=PLATFORM_COMMISSION_RATE)
    escrow_status = models.CharField(max_length=20, choices=EscrowStatus.choices, default=EscrowStatus.HELD)
    payment_method = models.CharField(max_length=20, choices=PaymentMethod.choices, default=PaymentMethod.CARD)
    # Retain legacy authorization references and the already-applied 0003 migration.
    # New website checkout is simulated; this field must never initiate a charge.
    stripe_payment_intent_id = models.CharField(max_length=255, blank=True, default='')
    delivery_confirmed = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return f"Txn #{self.id} - {self.listing.title} ({self.escrow_status})"

    @property
    def commission_amount(self):
        return (self.amount * self.commission_rate).quantize(Decimal('0.01'))

    @property
    def seller_payout(self):
        return self.amount - self.commission_amount
