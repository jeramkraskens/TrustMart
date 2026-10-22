# Django admin registration for buyer complaints and moderator decisions about orders.
# This configures the built-in /admin/ site, separate from the React admin dashboard.
from django.contrib import admin
from .models import Dispute

admin.site.register(Dispute)
