# Django admin registration for messages exchanged between marketplace users.
# This configures the built-in /admin/ site, separate from the React admin dashboard.
from django.contrib import admin
from .models import Message

admin.site.register(Message)
