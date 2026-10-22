from datetime import timedelta
from io import BytesIO
from tempfile import TemporaryDirectory
from PIL import Image
from django.contrib.auth import get_user_model
from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings
from django.utils import timezone
from rest_framework.test import APITestCase, APIClient
from .models import Advertisement


class AdvertisementWorkflowTests(APITestCase):
    def setUp(self):
        self.media = TemporaryDirectory()
        self.addCleanup(self.media.cleanup)
        override = override_settings(MEDIA_ROOT=self.media.name)
        override.enable()
        self.addCleanup(override.disable)
        self.admin = get_user_model().objects.create_user(username='ads_admin', role='admin')
        self.client.force_authenticate(self.admin)
        self.public = APIClient()

    def public_ads(self, client=None, params=None):
        response = (client or self.public).get('/api/advertisements/', params or {})
        self.assertEqual(response.status_code, 200)
        return response.data['results']

    def post_ad(self, fields):
        image = BytesIO()
        Image.new('RGB', (20, 10), 'teal').save(image, format='PNG')
        data = {'description': 'Verified promotion', 'link_url': 'https://example.com/offer',
                'image': SimpleUploadedFile('offer.png', image.getvalue(), content_type='image/png'), **fields}
        return self.client.post('/api/advertisements/', data, format='multipart')

    def test_create_upload_edit_toggle_and_delete(self):
        image = BytesIO()
        Image.new('RGB', (20, 10), 'teal').save(image, format='PNG')
        uploaded = SimpleUploadedFile('banner.png', image.getvalue(), content_type='image/png')
        response = self.post_ad({'title': 'Summer promotion', 'image': uploaded, 'is_active': 'true', 'sort_order': '2'})
        self.assertEqual(response.status_code, 201, response.data)
        url = f"/api/advertisements/{response.data['id']}/"
        original_image = response.data['image']
        self.assertTrue(original_image.endswith('.png'))
        self.assertEqual(len(self.public_ads()), 1)
        response = self.client.patch(url, {'title': 'Updated promotion', 'description': 'Good deals'}, format='multipart')
        self.assertEqual(response.status_code, 200, response.data)
        self.assertEqual(response.data['image'], original_image)
        self.assertEqual(self.public_ads()[0]['title'], 'Updated promotion')
        self.assertEqual(self.client.patch(url, {'is_active': False}, format='json').status_code, 200)
        self.assertEqual(self.public_ads(), [])
        self.assertEqual(self.public.get(url).status_code, 404)
        self.assertEqual(len(self.public_ads(self.client)), 1)
        self.assertEqual(self.client.patch(url, {'is_active': True}, format='json').status_code, 200)
        self.assertEqual(len(self.public_ads()), 1)
        self.assertEqual(self.client.delete(url).status_code, 204)
        self.assertEqual(self.public_ads(), [])

    def test_public_only_sees_active_current_ads_in_sort_order(self):
        now = timezone.now()
        first = Advertisement.objects.create(title='First', sort_order=0)
        last = Advertisement.objects.create(title='Last', sort_order=5)
        Advertisement.objects.create(title='Inactive', is_active=False)
        Advertisement.objects.create(title='Future', start_date=now + timedelta(days=1))
        Advertisement.objects.create(title='Expired', end_date=now - timedelta(days=1))
        expected = [first.pk, last.pk]
        self.assertEqual([item['id'] for item in self.public_ads()], expected)
        self.assertEqual([item['id'] for item in self.public_ads(self.client, {'active_only': 'true'})], expected)
        self.assertEqual(len(self.public_ads(self.client)), 5)

    def test_non_admin_cannot_change_advertisements(self):
        ad = Advertisement.objects.create(title='Protected')
        buyer = get_user_model().objects.create_user(username='ads_buyer', role='buyer')
        for user in [None, buyer]:
            self.public.force_authenticate(user)
            self.assertIn(self.public.post('/api/advertisements/', {'title': 'No'}).status_code, [401, 403])
            self.assertIn(self.public.patch(f'/api/advertisements/{ad.pk}/', {'is_active': False}).status_code, [401, 403])
            self.assertIn(self.public.delete(f'/api/advertisements/{ad.pk}/').status_code, [401, 403])

    def test_invalid_inputs_return_validation_errors(self):
        for payload in [{'title': ''}, {'title': 'Invalid order', 'sort_order': -1}, {'title': 'Invalid link', 'link_url': 'javascript:alert(1)'}]:
            self.assertEqual(self.client.post('/api/advertisements/', payload, format='json').status_code, 400)

    def test_required_content_is_not_optional(self):
        response = self.client.post('/api/advertisements/', {'title': 'Text promotion', 'is_active': 'false'}, format='multipart')
        self.assertEqual(response.status_code, 400)
        for field in ['description', 'link_url', 'image']:
            self.assertIn(field, response.data)
        self.assertEqual(self.public_ads(), [])

    def test_offer_price_is_saved_and_public_savings_are_calculated(self):
        response = self.post_ad({'title': 'Price deal', 'is_active': 'true', 'original_price': '10000', 'offer_price': '8000', 'discount_percent': ''})
        self.assertEqual(response.status_code, 201, response.data)
        ad = self.public_ads()[0]
        self.assertEqual(ad['original_price'], '10000.00')
        self.assertEqual(ad['effective_offer_price'], '8000.00')
        self.assertEqual(ad['effective_discount_percent'], '20.00')

    def test_percentage_discount_calculates_price_and_rounds_to_cents(self):
        response = self.post_ad({'title': 'Discount deal', 'is_active': 'true', 'original_price': '99.99', 'offer_price': '', 'discount_percent': '12.5'})
        self.assertEqual(response.status_code, 201, response.data)
        self.assertEqual(self.public_ads()[0]['effective_offer_price'], '87.49')
        self.assertEqual(self.public_ads()[0]['effective_discount_percent'], '12.50')

    def test_offer_can_be_changed_to_discount_and_removed(self):
        ad = Advertisement.objects.create(title='Change deal', description='Deal', link_url='https://example.com', image='existing.png', original_price=100, offer_price=80)
        url = f'/api/advertisements/{ad.pk}/'
        response = self.client.patch(url, {'offer_price': '', 'discount_percent': '25'}, format='multipart')
        self.assertEqual(response.status_code, 200, response.data)
        self.assertEqual(response.data['effective_offer_price'], '75.00')
        self.assertEqual(self.client.patch(url, {'is_active': False}, format='json').status_code, 200)
        response = self.client.patch(url, {'original_price': '', 'offer_price': '', 'discount_percent': ''}, format='multipart')
        self.assertEqual(response.status_code, 200, response.data)
        self.assertIsNone(response.data['effective_offer_price'])
        self.assertIsNone(response.data['effective_discount_percent'])
        ad.refresh_from_db()
        self.assertIsNone(ad.original_price)
        self.assertIsNone(ad.offer_price)
        self.assertIsNone(ad.discount_percent)

    def test_offer_or_discount_without_original_price(self):
        for fields in [{'offer_price': '0'}, {'discount_percent': '100'}]:
            response = self.post_ad({'title': 'Standalone offer', **fields})
            self.assertEqual(response.status_code, 400, response.data)
            self.assertIn('original_price', response.data)

    def test_invalid_offer_values_are_rejected(self):
        for fields in [
            {'offer_price': '-1'}, {'original_price': '0', 'offer_price': '0'},
            {'discount_percent': '-1'}, {'discount_percent': '0'}, {'discount_percent': '100.01'},
            {'original_price': '100', 'offer_price': '101'}, {'original_price': '100'},
            {'offer_price': '80', 'discount_percent': '20'}, {'offer_price': 'NaN'},
        ]:
            with self.subTest(fields=fields):
                response = self.post_ad({'title': 'Invalid offer', **fields})
                self.assertEqual(response.status_code, 400, response.data)

    def test_partial_update_validates_against_saved_prices(self):
        ad = Advertisement.objects.create(title='Existing offer', description='Deal', link_url='https://example.com', image='existing.png', original_price=100, offer_price=80)
        url = f'/api/advertisements/{ad.pk}/'
        self.assertEqual(self.client.patch(url, {'original_price': '50'}, format='json').status_code, 400)
        self.assertEqual(self.client.patch(url, {'discount_percent': '20'}, format='json').status_code, 400)

    def test_blank_fields_cannot_be_saved_but_old_ads_can_be_deactivated(self):
        ad = Advertisement.objects.create(title='Legacy incomplete ad')
        url = f'/api/advertisements/{ad.pk}/'
        self.assertEqual(self.client.patch(url, {'is_active': False}, format='json').status_code, 200)
        self.assertEqual(self.client.patch(url, {'is_active': True}, format='json').status_code, 400)
        for field in ['title', 'description', 'link_url']:
            response = self.post_ad({'title': 'Required check', field: '   '})
            self.assertEqual(response.status_code, 400)
            self.assertIn(field, response.data)
