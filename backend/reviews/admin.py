# Django admin registration for ratings and feedback associated with purchases.
# This configures the built-in /admin/ site, separate from the React admin dashboard.
from django.contrib import admin
from .models import Review

admin.site.register(Review)
