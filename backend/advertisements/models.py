# Database structure for promotional offers displayed in the marketplace.
# Django model fields describe stored values; relationships connect records in other apps.
from django.db import models
from django.core.validators import MinValueValidator, MaxValueValidator
from decimal import Decimal


class Advertisement(models.Model):
    title = models.CharField(max_length=150)
    image = models.ImageField(upload_to='advertisements/', blank=True, null=True)
    link_url = models.URLField(blank=True)
    description = models.CharField(max_length=255, blank=True)
    original_price = models.DecimalField(max_digits=12, decimal_places=2, null=True, blank=True,
        validators=[MinValueValidator(Decimal('0.01'))])
    offer_price = models.DecimalField(max_digits=12, decimal_places=2, null=True, blank=True,
        validators=[MinValueValidator(Decimal('0'))])
    discount_percent = models.DecimalField(max_digits=5, decimal_places=2, null=True, blank=True,
        validators=[MinValueValidator(Decimal('0.01')), MaxValueValidator(Decimal('100'))])
    is_active = models.BooleanField(default=True)
    sort_order = models.PositiveIntegerField(default=0)
    start_date = models.DateTimeField(null=True, blank=True)
    end_date = models.DateTimeField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ['sort_order', '-created_at']

    def __str__(self):
        return self.title
