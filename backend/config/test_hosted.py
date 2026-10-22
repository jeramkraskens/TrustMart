import tempfile
from pathlib import Path
from unittest.mock import patch
from django.core import signing
from django.core.files.base import ContentFile
from django.test import SimpleTestCase, override_settings
from .hosted_files import SignedMediaStorage


@override_settings(ROOT_URLCONF='config.hosted_urls', SECURE_SSL_REDIRECT=False)
class HostedDeliveryTests(SimpleTestCase):
    def test_media_links_expire_and_cannot_be_forged(self):
        with tempfile.TemporaryDirectory() as directory:
            with override_settings(MEDIA_ROOT=directory):
                storage = SignedMediaStorage()
                name = storage.save('verification_evidence/receipt.txt', ContentFile(b'private receipt'))
                with patch('django.core.signing.time.time', return_value=1000):
                    url = storage.url(name)
                with patch('django.core.signing.time.time', return_value=1001):
                    response = self.client.get(url)
                    self.assertEqual(response.status_code, 200)
                    self.assertEqual(b''.join(response.streaming_content), b'private receipt')
                    self.assertIn('attachment', response['Content-Disposition'])
                    response.close()
                with patch('django.core.signing.time.time', return_value=4601):
                    self.assertEqual(self.client.get(url).status_code, 404)
                self.assertEqual(self.client.get(url + 'forged').status_code, 404)
                self.assertEqual(self.client.get('/media/' + name).status_code, 404)

    def test_spa_routes_do_not_hide_missing_api_routes(self):
        with tempfile.TemporaryDirectory() as directory:
            Path(directory, 'index.html').write_text('<html>TrustMart</html>')
            with override_settings(FRONTEND_DIST=Path(directory)):
                response = self.client.get('/admin/overview')
                self.assertEqual(response.status_code, 200)
                response.close()
                self.assertEqual(self.client.get('/api/not-a-real-endpoint/').status_code, 404)
