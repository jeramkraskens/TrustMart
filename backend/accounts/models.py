# Database structure for user accounts, roles and profile details.
# Django model fields describe stored values; relationships connect records in other apps.
from django.contrib.auth.models import AbstractUser, UserManager
from django.db import models


class CustomUserManager(UserManager):
    """Same as Django's default manager, except superusers also get role='admin'
    so they can actually see/use the admin panel (is_superuser alone isn't enough
    on the frontend, which checks the `role` field)."""

    def create_superuser(self, username, email=None, password=None, **extra_fields):
        extra_fields.setdefault('role', 'admin')
        return super().create_superuser(username, email, password, **extra_fields)


# Section: User data — account roles, contact details and seller identity.
class User(AbstractUser):
    class Role(models.TextChoices):
        BUYER = 'buyer', 'Buyer'
        SELLER = 'seller', 'Seller'
        MODERATOR = 'moderator', 'Moderator'
        ADMIN = 'admin', 'Admin'

    role = models.CharField(max_length=20, choices=Role.choices, default=Role.BUYER)
    phone = models.CharField(max_length=20, blank=True)
    district = models.CharField(max_length=40, blank=True)
    # Section: NIC storage — preserve leading zeros without a fixed digit count; allow no suffix.
    nic_number = models.TextField(blank=True)
    nic_suffix = models.CharField(max_length=1, choices=[('V', 'V'), ('X', 'X')], blank=True)
    trust_score = models.IntegerField(default=100)
    is_banned = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)

    objects = CustomUserManager()

    def __str__(self):
        return f"{self.username} ({self.role})"
