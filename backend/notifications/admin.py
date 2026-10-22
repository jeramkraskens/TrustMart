# Django admin registration for in-app notices about marketplace activity.
# This configures the built-in /admin/ site, separate from the React admin dashboard.
from django.contrib import admin
from .models import Notification

admin.site.register(Notification)
