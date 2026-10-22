"""Regression coverage for metadata consumed by the Android review screens."""
from django.contrib.auth import get_user_model
from django.test import TestCase
from rest_framework.test import APIRequestFactory
from listings.models import Category, Listing, ListingImage
from transactions.models import Transaction
from disputes.models import Dispute
from disputes.serializers import DisputeSerializer
from .models import VerificationRequest
from .serializers import VerificationRequestSerializer


class MobileMetadataTests(TestCase):
    def setUp(self):
        self.seller = get_user_model().objects.create_user(username='seller', role='seller')
        self.buyer = get_user_model().objects.create_user(username='buyer', role='buyer')
        category = Category.objects.create(name='Phones')
        self.listing = Listing.objects.create(seller=self.seller, category=category,
            title='Phone', description='Actual product', price='245000.00')
        ListingImage.objects.create(listing=self.listing, image='listings/phone.jpg')
        self.review = VerificationRequest.objects.create(listing=self.listing)

    def test_review_has_read_only_product_metadata_and_absolute_photo_url(self):
        request = APIRequestFactory().get('/', HTTP_HOST='localhost:8000')
        serializer = VerificationRequestSerializer(self.review, context={'request': request})
        detail = serializer.data['listing_detail']
        self.assertEqual(detail['price'], '245000.00')
        self.assertEqual(detail['category_name'], 'Phones')
        self.assertEqual(detail['images'][0]['image'], 'http://localhost:8000/media/listings/phone.jpg')
        self.assertTrue(serializer.fields['listing_detail'].read_only)

    def test_dispute_contains_product_metadata_without_losing_existing_fields(self):
        transaction = Transaction.objects.create(buyer=self.buyer, listing=self.listing, amount='245000.00')
        dispute = Dispute.objects.create(transaction=transaction, raised_by=self.buyer, reason='Wrong item')
        data = DisputeSerializer(dispute).data
        self.assertEqual(data['listing_detail']['title'], 'Phone')
        self.assertEqual(data['buyer_id'], self.buyer.id)
        self.assertEqual(data['seller_id'], self.seller.id)
        self.assertEqual(data['status'], 'open')
