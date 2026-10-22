# Database structure for ratings and feedback associated with purchases.
# Django model fields describe stored values; relationships connect records in other apps.
from django.conf import settings
from django.core.validators import MaxValueValidator, MinValueValidator
from django.db import models
from transactions.models import Transaction


class Review(models.Model):
    transaction = models.ForeignKey(Transaction, on_delete=models.CASCADE, related_name='reviews')
    reviewer = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='reviews_given')
    reviewee = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='reviews_received')
    rating = models.IntegerField(validators=[MinValueValidator(1), MaxValueValidator(5)])
    comment = models.TextField(blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-created_at']
        unique_together = ['transaction', 'reviewer']

    def __str__(self):
        return f"{self.rating} stars: {self.reviewer} -> {self.reviewee}"
