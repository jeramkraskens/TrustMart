from rest_framework.test import APITestCase
from accounts.models import User
from .models import Category, Listing


class SearchSuggestionsTests(APITestCase):
    def setUp(self):
        self.seller = User.objects.create_user(username='suggestion_seller', role='seller')
        self.category = Category.objects.create(name='Mobile Phones')
        self.public = Listing.objects.create(seller=self.seller, category=self.category,
            title='Samsung Galaxy Phone', description='Excellent camera', price=100, status='verified')
        for status in ['draft', 'pending_verification', 'rejected', 'sold']:
            Listing.objects.create(seller=self.seller, title=f'Secret {status}', price=100, status=status)

    def test_public_matches_and_does_not_expose_contact_data(self):
        response = self.client.get('/api/listings/suggestions/?q=samsung')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data[0]['url'], f'/listings/{self.public.pk}')
        self.assertNotIn('contact_phone', response.data[0])

    def test_drafts_hidden_even_from_owner_and_admin(self):
        for user in [None, self.seller, User.objects.create_user(username='staff', role='admin')]:
            self.client.force_authenticate(user)
            data = self.client.get('/api/listings/suggestions/?q=secret&all=true&status=draft').data
            self.assertEqual(data, [])

    def test_case_whitespace_and_multiple_terms(self):
        data = self.client.get('/api/listings/suggestions/', {'q': '  SAMSUNG   camera '}).data
        self.assertEqual(data[0]['id'], f'listing-{self.public.pk}')

    def test_category_and_town_destinations(self):
        data = self.client.get('/api/listings/suggestions/?q=mobile').data
        self.assertTrue(any(item['url'] == f'/browse?category={self.category.pk}' for item in data))
        data = self.client.get('/api/listings/suggestions/?q=nugegoda').data
        self.assertTrue(any('district=Colombo&city=Nugegoda' in item['url'] for item in data))
        data = self.client.get('/api/listings/suggestions/?q=kandy').data
        self.assertTrue(any(item['url'] == '/browse?district=Kandy' for item in data))

    def test_results_are_bounded_and_no_match_is_empty(self):
        for i in range(15):
            Listing.objects.create(seller=self.seller, title=f'Test phone {i}', price=100, status='verified')
        data = self.client.get('/api/listings/suggestions/').data
        self.assertLessEqual(len([item for item in data if item['kind'] == 'Listing']), 6)
        self.assertEqual(self.client.get('/api/listings/suggestions/?q=zzzznomatch').data, [])
