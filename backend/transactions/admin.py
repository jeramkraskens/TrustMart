# Django admin registration for orders and simulated escrow payment states.
# This configures the built-in /admin/ site, separate from the React admin dashboard.
from django.contrib import admin
from .models import Transaction

admin.site.register(Transaction)
