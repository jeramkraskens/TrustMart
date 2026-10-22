# API data conversion for ratings and feedback associated with purchases.
# Serializers turn model records into response data and validate incoming fields before saving.
from rest_framework import serializers
from .models import Review


class ReviewSerializer(serializers.ModelSerializer):
    reviewer_name = serializers.CharField(source='reviewer.username', read_only=True)
    reviewee_name = serializers.CharField(source='reviewee.username', read_only=True)

    class Meta:
        model = Review
        fields = ['id', 'transaction', 'reviewer', 'reviewer_name', 'reviewee', 'reviewee_name',
                  'rating', 'comment', 'created_at']
        read_only_fields = ['id', 'reviewer', 'created_at']
