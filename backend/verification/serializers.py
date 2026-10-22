# API data conversion for moderator reviews of listing evidence.
# Serializers turn model records into response data and validate incoming fields before saving.
from rest_framework import serializers
from .models import VerificationRequest
from listings.serializers import ListingSerializer


class VerificationRequestSerializer(serializers.ModelSerializer):
    def validate_listing(self, listing):
        if listing.seller_id != self.context['request'].user.pk:
            raise serializers.ValidationError('Only the seller can submit this listing for verification.')
        if listing.status == 'sold' or listing.transactions.exists():
            raise serializers.ValidationError('A listing with an order cannot be submitted for verification.')
        return listing

    # Additive mobile compatibility: metadata is read-only and follows the request's access checks.
    listing_detail = ListingSerializer(source='listing', read_only=True)
    listing_title = serializers.CharField(source='listing.title', read_only=True)
    seller_name = serializers.CharField(source='listing.seller.username', read_only=True)
    moderator_name = serializers.CharField(source='moderator.username', read_only=True)

    class Meta:
        model = VerificationRequest
        fields = ['id', 'listing', 'listing_detail', 'listing_title', 'seller_name', 'moderator', 'moderator_name',
                  'evidence', 'notes', 'status', 'feedback', 'created_at', 'reviewed_at']
        read_only_fields = ['id', 'moderator', 'status', 'feedback', 'created_at', 'reviewed_at']
