# API data conversion for buyer complaints and moderator decisions about orders.
# Serializers turn model records into response data and validate incoming fields before saving.
from rest_framework import serializers
from .models import Dispute
from listings.serializers import ListingSerializer


class DisputeSerializer(serializers.ModelSerializer):
    def validate_transaction(self, txn):
        if self.context['request'].user.pk not in (txn.buyer_id, txn.listing.seller_id):
            raise serializers.ValidationError('Only order participants can open a dispute.')
        if txn.escrow_status != 'held':
            raise serializers.ValidationError('This order is already settled.')
        return txn

    listing_detail = ListingSerializer(source='transaction.listing', read_only=True)
    raised_by_name = serializers.CharField(source='raised_by.username', read_only=True)
    listing_title = serializers.CharField(source='transaction.listing.title', read_only=True)
    buyer_name = serializers.CharField(source='transaction.buyer.username', read_only=True)
    buyer_id = serializers.IntegerField(source='transaction.buyer.id', read_only=True)
    seller_name = serializers.CharField(source='transaction.listing.seller.username', read_only=True)
    seller_id = serializers.IntegerField(source='transaction.listing.seller.id', read_only=True)
    amount = serializers.DecimalField(source='transaction.amount', max_digits=12, decimal_places=2, read_only=True)

    class Meta:
        model = Dispute
        fields = ['id', 'transaction', 'listing_detail', 'listing_title', 'amount', 'raised_by', 'raised_by_name',
                  'buyer_name', 'buyer_id', 'seller_name', 'seller_id', 'moderator',
                  'reason', 'status', 'resolution', 'resolution_notes', 'created_at', 'resolved_at']
        read_only_fields = ['id', 'raised_by', 'moderator', 'status', 'resolution',
                             'resolution_notes', 'created_at', 'resolved_at']
