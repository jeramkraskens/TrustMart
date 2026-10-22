# Django admin registration for moderator reviews of listing evidence.
# This configures the built-in /admin/ site, separate from the React admin dashboard.
from django.contrib import admin
from .models import VerificationRequest

admin.site.register(VerificationRequest)
