from django.db.models.signals import post_save
from django.dispatch import receiver
from verification.models import VerificationRequest
from .utils import notify, notify_staff


@receiver(post_save, sender=VerificationRequest, dispatch_uid='notify_verification_submission')
def verification_submitted(sender, instance, created, raw=False, **kwargs):
    # Listings can submit or resubmit evidence through either the listing or verification API.
    if not created or raw:
        return
    notify(instance.listing.seller, 'verification_submitted',
           f'Your listing "{instance.listing.title}" was submitted for verification.', link='/seller')
    notify_staff('verification_pending', f'Listing "{instance.listing.title}" needs verification.', link='/moderator')
