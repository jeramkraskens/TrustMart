# Database structure for recorded searches, listing views and product suggestions.
# Django model fields describe stored values; relationships connect records in other apps.
from django.conf import settings
from django.db import models


class SearchQuery(models.Model):
    """Logs every search a buyer makes — this is the training data for the recommender."""
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='search_queries')
    query_text = models.CharField(max_length=255)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-created_at']
        verbose_name_plural = 'Search queries'

    def __str__(self):
        return f'{self.user.username}: "{self.query_text}"'


class ListingView(models.Model):
    """Logs when a buyer views a listing's detail page — additional training signal."""
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='listing_views')
    listing = models.ForeignKey('listings.Listing', on_delete=models.CASCADE, related_name='views')
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return f'{self.user.username} viewed {self.listing.title}'
