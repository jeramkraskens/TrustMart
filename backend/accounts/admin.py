# Django admin registration for user accounts, roles and profile details.
# This configures the built-in /admin/ site, separate from the React admin dashboard.
from django.contrib import admin
from django.contrib.auth.admin import UserAdmin
from .models import User


class CustomUserAdmin(UserAdmin):
    list_display = ['username', 'email', 'role', 'trust_score', 'is_banned', 'is_staff']
    fieldsets = UserAdmin.fieldsets + (
        ('Marketplace info', {'fields': ('role', 'phone', 'trust_score', 'is_banned')}),
    )


admin.site.register(User, CustomUserAdmin)
