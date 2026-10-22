import io
import tempfile
from PIL import Image
from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings
from rest_framework.test import APITestCase
from accounts.models import User
from .models import Category, Listing, ListingImage
from transactions.models import Transaction
from verification.models import VerificationRequest


class ListingManagementTests(APITestCase):
    def setUp(self):
        self.media = tempfile.TemporaryDirectory()
        self.addCleanup(self.media.cleanup)
        self.override = override_settings(MEDIA_ROOT=self.media.name)
        self.override.enable()
        self.addCleanup(self.override.disable)
        self.seller = User.objects.create_user(username='seller', role='seller')
        self.other = User.objects.create_user(username='other', role='seller')
        self.buyer = User.objects.create_user(username='buyer', role='buyer')
        self.moderator = User.objects.create_user(username='moderator', role='moderator')
        self.listing = Listing.objects.create(seller=self.seller, title='Phone', description='Good phone', price=100, status='verified')
        self.url = f'/api/listings/{self.listing.pk}/'
        self.client.force_authenticate(self.seller)

    def photo(self):
        output = io.BytesIO()
        Image.new('RGB', (8, 8), 'teal').save(output, format='PNG')
        return SimpleUploadedFile('photo.png', output.getvalue(), content_type='image/png')

    def test_locations_are_public_and_complete(self):
        self.client.force_authenticate(None)
        response = self.client.get('/api/listings/locations/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data), 25)
        self.assertIn('Nugegoda', response.data['Colombo'])

    def test_create_with_location_contact_and_review_is_atomic(self):
        response = self.client.post('/api/listings/', {
            'title': 'Camera', 'description': 'Camera with receipt', 'price': '500',
            'district': 'Colombo', 'city': 'Nugegoda', 'contact_phone': '0771234567',
            'condition': 'good', 'category': Category.objects.create(name='Cameras').pk,
            'evidence_file': self.photo(),
            'whatsapp_enabled': 'true', 'evidence_notes': 'Receipt attached',
            'uploaded_images': [self.photo()],
        }, format='multipart')
        self.assertEqual(response.status_code, 201, response.data)
        self.assertEqual(response.data['contact_phone'], '0771234567')
        self.assertEqual(response.data['location'], 'Nugegoda, Colombo')
        self.assertEqual(response.data['status'], 'pending_verification')
        self.assertEqual(len(response.data['images']), 1)
        self.assertTrue(VerificationRequest.objects.filter(listing_id=response.data['id'], status='pending').exists())

    def test_invalid_location_and_phone_are_rejected(self):
        for fields in [
            {'district': 'Colombo', 'city': 'Kandy'},
            {'contact_phone': 'abc'},
            {'whatsapp_enabled': True, 'contact_phone': ''},
        ]:
            response = self.client.patch(self.url, fields, format='json')
            self.assertEqual(response.status_code, 400, response.data)
        self.listing.refresh_from_db()
        self.assertEqual(self.listing.status, 'verified')

    def test_create_requires_all_fields_and_strict_phone(self):
        category = Category.objects.create(name='Required category')
        def payload():
            return {'title': 'Camera', 'description': 'Good camera', 'price': '500', 'condition': 'good',
                    'category': category.pk, 'district': 'Colombo', 'city': 'Nugegoda',
                    'contact_phone': '0771234567', 'evidence_notes': 'Original receipt',
                    'evidence_file': self.photo(), 'uploaded_images': [self.photo()]}
        for key in payload():
            data = payload()
            data.pop(key)
            response = self.client.post('/api/listings/', data, format='multipart')
            self.assertEqual(response.status_code, 400, key)
            self.assertIn(key, response.data)
        for phone in ('+94771234567', '077 1234567', '077123456', '07712345678', '077123456a'):
            data = payload()
            data['contact_phone'] = phone
            response = self.client.post('/api/listings/', data, format='multipart')
            self.assertEqual(response.status_code, 400, phone)
            self.assertIn('contact_phone', response.data)
        self.assertEqual(Listing.objects.count(), 1)

    def test_cannot_remove_last_product_photo(self):
        photo = ListingImage.objects.create(listing=self.listing, image=self.photo())
        response = self.client.patch(self.url, {'removed_image_ids': [photo.pk]}, format='json')
        self.assertEqual(response.status_code, 400)
        self.assertTrue(ListingImage.objects.filter(pk=photo.pk).exists())

    def test_owner_edit_preserves_photos_and_queues_review(self):
        photo = ListingImage.objects.create(listing=self.listing, image=self.photo())
        previous = VerificationRequest.objects.create(listing=self.listing, status='approved', notes='Original receipt')
        response = self.client.patch(self.url, {'title': 'Updated phone'}, format='multipart')
        self.assertEqual(response.status_code, 200, response.data)
        self.assertEqual(response.data['status'], 'pending_verification')
        self.assertEqual(response.data['images'][0]['id'], photo.pk)
        self.assertEqual(self.listing.verification_requests.first().notes, previous.notes)

    def test_edit_can_add_and_remove_photos(self):
        photo = ListingImage.objects.create(listing=self.listing, image=self.photo())
        response = self.client.patch(self.url, {'removed_image_ids': [photo.pk], 'uploaded_images': [self.photo()]}, format='multipart')
        self.assertEqual(response.status_code, 200, response.data)
        self.assertEqual(len(response.data['images']), 1)
        self.assertNotEqual(response.data['images'][0]['id'], photo.pk)

    def test_cannot_remove_another_listings_photo(self):
        other_listing = Listing.objects.create(seller=self.other, title='Other', price=10)
        photo = ListingImage.objects.create(listing=other_listing, image=self.photo())
        response = self.client.patch(self.url, {'removed_image_ids': [photo.pk]}, format='multipart')
        self.assertEqual(response.status_code, 400)
        self.assertTrue(ListingImage.objects.filter(pk=photo.pk).exists())

    def test_non_owner_cannot_edit_or_delete(self):
        self.client.force_authenticate(self.other)
        self.assertEqual(self.client.patch(self.url, {'title': 'Not mine'}).status_code, 403)
        self.assertEqual(self.client.delete(self.url).status_code, 403)

    def test_owner_can_delete_without_orders(self):
        self.assertEqual(self.client.delete(self.url).status_code, 204)
        self.assertFalse(Listing.objects.filter(pk=self.listing.pk).exists())

    def test_orders_prevent_edit_and_deletion_even_if_status_is_verified(self):
        order = Transaction.objects.create(listing=self.listing, buyer=self.buyer, amount=100)
        self.assertFalse(self.client.get(self.url).data['can_manage'])
        self.assertEqual(self.client.patch(self.url, {'title': 'Changed'}).status_code, 400)
        self.assertEqual(self.client.delete(self.url).status_code, 400)
        self.assertTrue(Transaction.objects.filter(pk=order.pk).exists())

    def test_edit_supersedes_pending_review(self):
        self.listing.status = 'pending_verification'
        self.listing.save()
        old = VerificationRequest.objects.create(listing=self.listing)
        self.assertEqual(self.client.patch(self.url, {'title': 'New title'}).status_code, 200)
        self.client.force_authenticate(self.moderator)
        self.assertEqual(self.client.post(f'/api/verifications/{old.pk}/approve/').status_code, 400)
        current = self.listing.verification_requests.first()
        self.assertEqual(self.client.post(f'/api/verifications/{current.pk}/approve/').status_code, 200)

    def test_location_filter(self):
        self.listing.district = 'Colombo'
        self.listing.city = 'Nugegoda'
        self.listing.save()
        self.assertEqual(self.client.get('/api/listings/?district=Colombo&city=Nugegoda').data['count'], 1)
        self.assertEqual(self.client.get('/api/listings/?district=Kandy').data['count'], 0)

    def test_edited_listing_cannot_be_purchased_until_reviewed(self):
        self.client.patch(self.url, {'title': 'Updated'})
        self.client.force_authenticate(self.buyer)
        response = self.client.post('/api/transactions/', {'listing': self.listing.pk, 'payment_method': 'card'})
        self.assertEqual(response.status_code, 400, response.data)
        self.assertFalse(self.listing.transactions.exists())
