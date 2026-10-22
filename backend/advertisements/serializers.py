# API data conversion for promotional offers displayed in the marketplace.
# Serializers turn model records into response data and validate incoming fields before saving.
from rest_framework import serializers
from decimal import Decimal, ROUND_HALF_UP
from .models import Advertisement


# Section: Advertisement data — validate fields and calculate displayed savings.
class AdvertisementSerializer(serializers.ModelSerializer):
    effective_offer_price = serializers.SerializerMethodField()
    effective_discount_percent = serializers.SerializerMethodField()

    def get_effective_offer_price(self, obj):
        if obj.offer_price is not None:
            return str(obj.offer_price)
        if obj.original_price is not None and obj.discount_percent is not None:
            amount = obj.original_price * (1 - obj.discount_percent / 100)
            return str(amount.quantize(Decimal('0.01'), rounding=ROUND_HALF_UP))
        return None

    def get_effective_discount_percent(self, obj):
        if obj.discount_percent is not None:
            return str(obj.discount_percent)
        if obj.original_price and obj.offer_price is not None:
            percent = (1 - obj.offer_price / obj.original_price) * 100
            return str(percent.quantize(Decimal('0.01'), rounding=ROUND_HALF_UP))
        return None

    def validate(self, attrs):
        if self.instance and attrs == {'is_active': False}:
            return attrs
        # Permit deactivating an old incomplete banner, but validate creation,
        # activation and edits against the complete resulting advertisement.
        if not self.instance or set(attrs) != {'is_active'} or attrs.get('is_active'):
            errors = {}
            for field, message in [('title', 'Enter a title.'), ('description', 'Enter a description.'),
                                   ('link_url', 'Enter a link URL.'), ('image', 'Add a banner image.')]:
                value = attrs.get(field, getattr(self.instance, field, None))
                if not value or isinstance(value, str) and not value.strip():
                    errors[field] = message
            if errors:
                raise serializers.ValidationError(errors)
        original = attrs.get('original_price', getattr(self.instance, 'original_price', None))
        offer = attrs.get('offer_price', getattr(self.instance, 'offer_price', None))
        discount = attrs.get('discount_percent', getattr(self.instance, 'discount_percent', None))
        if (offer is not None or discount is not None) and original is None:
            raise serializers.ValidationError({'original_price': 'Enter the original price for this offer.'})
        if offer is not None and discount is not None:
            raise serializers.ValidationError('Choose an offer price or a discount percentage, not both.')
        if original is not None and offer is not None and offer > original:
            raise serializers.ValidationError({'offer_price': 'Offer price cannot exceed the original price.'})
        if original is not None and offer is None and discount is None:
            raise serializers.ValidationError({'original_price': 'Add an offer price or discount, or clear the original price.'})
        return attrs

    def validate_link_url(self, value):
        if value and not value.lower().startswith(('http://', 'https://')):
            raise serializers.ValidationError('Use an http:// or https:// link.')
        return value

    class Meta:
        model = Advertisement
        fields = ['id', 'title', 'image', 'link_url', 'description', 'is_active',
                  'sort_order', 'start_date', 'end_date', 'created_at', 'updated_at',
                  'original_price', 'offer_price', 'discount_percent',
                  'effective_offer_price', 'effective_discount_percent']
        read_only_fields = ['id', 'created_at', 'updated_at']
