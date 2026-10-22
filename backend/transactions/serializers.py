# API data conversion for orders and simulated escrow payment states.
# Serializers turn model records into response data and validate incoming fields before saving.
from rest_framework import serializers
from .models import Transaction


class TransactionSerializer(serializers.ModelSerializer):
    listing_title = serializers.CharField(source='listing.title', read_only=True)
    seller_name = serializers.CharField(source='listing.seller.username', read_only=True)
    seller_id = serializers.IntegerField(source='listing.seller.id', read_only=True)
    buyer_name = serializers.CharField(source='buyer.username', read_only=True)
    commission_amount = serializers.DecimalField(max_digits=12, decimal_places=2, read_only=True)
    seller_payout = serializers.DecimalField(max_digits=12, decimal_places=2, read_only=True)

    class Meta:
        model = Transaction
        fields = ['id', 'buyer', 'buyer_name', 'listing', 'listing_title', 'seller_name', 'seller_id', 'amount',
                  'commission_rate', 'commission_amount', 'seller_payout',
                  'escrow_status', 'payment_method', 'delivery_confirmed', 'created_at', 'updated_at']
        read_only_fields = ['id', 'buyer', 'amount', 'commission_rate', 'escrow_status',
                             'delivery_confirmed', 'created_at', 'updated_at']
