# Django admin registration for promotional offers displayed in the marketplace.
# This configures the built-in /admin/ site, separate from the React admin dashboard.
from django.contrib import admin
from .models import Advertisement

admin.site.register(Advertisement)
