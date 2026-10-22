# Database structure for moderator reviews of listing evidence.
# Django model fields describe stored values; relationships connect records in other apps.
from django.conf import settings
from django.db import models
from listings.models import Listing


class VerificationRequest(models.Model):
    class Status(models.TextChoices):
        PENDING = 'pending', 'Pending'
        APPROVED = 'approved', 'Approved'
        REJECTED = 'rejected', 'Rejected'

    listing = models.ForeignKey(Listing, on_delete=models.CASCADE, related_name='verification_requests')
    moderator = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.SET_NULL, null=True, blank=True,
                                    related_name='moderated_verifications')
    evidence = models.FileField(upload_to='verification_evidence/', blank=True, null=True)
    notes = models.TextField(blank=True, help_text='Seller notes describing proof of authenticity/condition')
    status = models.CharField(max_length=20, choices=Status.choices, default=Status.PENDING)
    feedback = models.TextField(blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    reviewed_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return f"Verification for {self.listing.title} - {self.status}"
