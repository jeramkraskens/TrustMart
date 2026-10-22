# Django admin registration for recorded searches, listing views and product suggestions.
# This configures the built-in /admin/ site, separate from the React admin dashboard.
from django.contrib import admin
from .models import SearchQuery, ListingView

admin.site.register(SearchQuery)
admin.site.register(ListingView)
