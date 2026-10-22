# API data conversion for product listings, categories, photos and saved items.
# Serializers turn model records into response data and validate incoming fields before saving.
from django.db import transaction
from rest_framework import serializers
from accounts.validators import validate_phone
from .models import Category, Listing, ListingImage, Wishlist
from .locations import LOCATIONS


class CategorySerializer(serializers.ModelSerializer):
    class Meta:
        model = Category
        fields = ['id', 'name', 'description']


class ListingImageSerializer(serializers.ModelSerializer):
    class Meta:
        model = ListingImage
        fields = ['id', 'image', 'uploaded_at']


class ListingSerializer(serializers.ModelSerializer):
    contact_phone = serializers.CharField(required=True, trim_whitespace=False, validators=[validate_phone])
    can_manage = serializers.SerializerMethodField()
    removed_image_ids = serializers.ListField(child=serializers.IntegerField(), write_only=True, required=False)
    evidence_notes = serializers.CharField(write_only=True, required=False, allow_blank=True)
    evidence_file = serializers.FileField(write_only=True, required=False)
    images = ListingImageSerializer(many=True, read_only=True)
    seller_name = serializers.CharField(source='seller.username', read_only=True)
    seller_trust_score = serializers.IntegerField(source='seller.trust_score', read_only=True)
    category_name = serializers.CharField(source='category.name', read_only=True)
    uploaded_images = serializers.ListField(
        child=serializers.ImageField(), write_only=True, required=False
    )

    class Meta:
        model = Listing
        fields = ['id', 'seller', 'seller_name', 'seller_trust_score', 'category', 'category_name',
                   'title', 'description', 'price', 'condition', 'status', 'location',
                   'district', 'city', 'contact_phone', 'whatsapp_enabled', 'can_manage',
                   'created_at', 'updated_at', 'images', 'uploaded_images', 'removed_image_ids',
                   'evidence_notes', 'evidence_file']
        # Clients can see these fields, but cannot directly choose their values through this serializer.
        read_only_fields = ['id', 'seller', 'status', 'created_at', 'updated_at']

    def get_can_manage(self, obj):
        return obj.status != Listing.Status.SOLD and not obj.transactions.exists()

    # Section: Listing validation — required fields, location, contact and evidence.
    def validate(self, attrs):
        required = ('title', 'description', 'category', 'price', 'condition', 'district', 'city', 'contact_phone')
        errors = {key: 'This field is required.' for key in required
                  if (not self.instance or key in attrs) and attrs.get(key) in (None, '')}
        if attrs.get('price', 0) < 0:
            errors['price'] = 'Price cannot be negative.'
        if not self.instance:
            for key in ('uploaded_images', 'evidence_notes', 'evidence_file'):
                if not attrs.get(key):
                    errors[key] = 'This field is required.'
        if errors:
            raise serializers.ValidationError(errors)
        district = attrs.get('district', getattr(self.instance, 'district', ''))
        city = attrs.get('city', getattr(self.instance, 'city', ''))
        if district or city:
            if district not in LOCATIONS:
                raise serializers.ValidationError({'district': 'Choose a valid Sri Lankan district.'})
            if city not in LOCATIONS[district]:
                raise serializers.ValidationError({'city': 'Choose a town in the selected district.'})
            attrs['location'] = f'{city}, {district}' if city != district else district
        elif 'district' in attrs or 'city' in attrs:
            attrs['location'] = ''
        phone = attrs.get('contact_phone', getattr(self.instance, 'contact_phone', ''))
        whatsapp = attrs.get('whatsapp_enabled', getattr(self.instance, 'whatsapp_enabled', False))
        if whatsapp and not phone:
            raise serializers.ValidationError({'contact_phone': 'Add a phone number to enable WhatsApp.'})
        removed = set(attrs.get('removed_image_ids', []))
        owned = set(self.instance.images.values_list('id', flat=True)) if self.instance else set()
        if not removed.issubset(owned):
            raise serializers.ValidationError({'removed_image_ids': 'Only photos belonging to this listing can be removed.'})
        if removed and not (owned - removed) and not attrs.get('uploaded_images'):
            raise serializers.ValidationError({'uploaded_images': 'Keep or upload at least one product photo.'})
        return attrs

    # Section: Listing creation — save the item, photos and verification request together.
    @transaction.atomic
    def create(self, validated_data):
        images = validated_data.pop('uploaded_images', [])
        validated_data.pop('removed_image_ids', None)
        submit_for_review = 'evidence_notes' in validated_data or 'evidence_file' in validated_data
        notes = validated_data.pop('evidence_notes', '')
        evidence = validated_data.pop('evidence_file', None)
        listing = Listing.objects.create(**validated_data)
        for img in images:
            ListingImage.objects.create(listing=listing, image=img)
        if submit_for_review:
            from verification.models import VerificationRequest
            VerificationRequest.objects.create(listing=listing, notes=notes, evidence=evidence)
            listing.status = Listing.Status.PENDING
            listing.save(update_fields=['status'])
        return listing

    # Section: Listing edits — retain existing evidence and submit a fresh review.
    @transaction.atomic
    def update(self, instance, validated_data):
        from verification.models import VerificationRequest
        images = validated_data.pop('uploaded_images', [])
        removed = validated_data.pop('removed_image_ids', [])
        notes = validated_data.pop('evidence_notes', None)
        evidence = validated_data.pop('evidence_file', None)
        instance = super().update(instance, validated_data)
        instance.images.filter(id__in=removed).delete()
        for img in images:
            ListingImage.objects.create(listing=instance, image=img)
        previous = instance.verification_requests.first()
        instance.verification_requests.filter(status='pending').update(
            status='rejected', feedback='Superseded by an edited listing.')
        VerificationRequest.objects.create(
            listing=instance,
            notes=notes if notes is not None else (previous.notes if previous else ''),
            evidence=evidence or (previous.evidence if previous else None),
        )
        instance.status = Listing.Status.PENDING
        instance.save(update_fields=['status', 'updated_at'])
        return instance


class WishlistSerializer(serializers.ModelSerializer):
    listing_detail = ListingSerializer(source='listing', read_only=True)

    class Meta:
        model = Wishlist
        fields = ['id', 'listing', 'listing_detail', 'created_at']
        read_only_fields = ['id', 'created_at']

    def create(self, validated_data):
        user = self.context['request'].user
        obj, _ = Wishlist.objects.get_or_create(user=user, listing=validated_data['listing'])
        return obj
