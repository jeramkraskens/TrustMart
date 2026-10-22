from rest_framework.test import APITestCase
from accounts.models import User
from .models import Listing


class PublicBrowseTests(APITestCase):
    @classmethod
    def setUpTestData(cls):
        seller = User.objects.create_user(username='public-test-seller', role='seller')
        cls.verified = Listing.objects.create(seller=seller, title='Verified phone', price=100, status='verified')
        cls.pending = Listing.objects.create(seller=seller, title='Private phone', price=100, status='pending_verification')

    def test_guest_can_browse_and_open_verified_item(self):
        response = self.client.get('/api/listings/?status=verified')
        self.assertEqual(response.status_code, 200)
        self.assertEqual([item['id'] for item in response.data['results']], [self.verified.pk])
        self.assertEqual(self.client.get(f'/api/listings/{self.verified.pk}/').status_code, 200)

    def test_guest_cannot_expose_unverified_items_with_filters(self):
        for query in ('', '?status=pending_verification', '?all=true', '?mine=true'):
            with self.subTest(query=query):
                response = self.client.get('/api/listings/' + query)
                self.assertEqual(response.status_code, 200)
                self.assertNotIn(self.pending.pk, [item['id'] for item in response.data['results']])
                response = self.client.get(f'/api/listings/{self.pending.pk}/' + query)
                self.assertEqual(response.status_code, 404)
