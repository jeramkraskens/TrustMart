# Database structure for buyer complaints and moderator decisions about orders.
# Django model fields describe stored values; relationships connect records in other apps.
from django.conf import settings
from django.db import models
from transactions.models import Transaction


class Dispute(models.Model):
    class Status(models.TextChoices):
        OPEN = 'open', 'Open'
        RESOLVED = 'resolved', 'Resolved'

    class Resolution(models.TextChoices):
        NONE = 'none', 'Not resolved yet'
        BUYER_FAVOR = 'buyer_favor', 'Refund to buyer'
        SELLER_FAVOR = 'seller_favor', 'Release to seller'

    transaction = models.ForeignKey(Transaction, on_delete=models.CASCADE, related_name='disputes')
    raised_by = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='disputes_raised')
    moderator = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.SET_NULL, null=True, blank=True,
                                    related_name='disputes_handled')
    reason = models.TextField()
    status = models.CharField(max_length=20, choices=Status.choices, default=Status.OPEN)
    resolution = models.CharField(max_length=20, choices=Resolution.choices, default=Resolution.NONE)
    resolution_notes = models.TextField(blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    resolved_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return f"Dispute #{self.id} - {self.status}"
