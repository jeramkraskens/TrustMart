from django.contrib.auth import get_user_model
from django.test import TestCase
from rest_framework.test import APIClient


class MobilePasswordContractTests(TestCase):
    def test_password_change_contract_for_buyer_seller_and_moderator(self):
        for role in ('buyer', 'seller', 'moderator'):
            with self.subTest(role=role):
                user = get_user_model().objects.create_user(username=role, role=role, password='BeforeTest42!')
                client = APIClient()
                client.force_authenticate(user)
                fields = {'old_password': 'BeforeTest42!', 'new_password': 'AfterTest73!'}
                self.assertEqual(client.post('/api/auth/change-password/', fields).status_code, 400)
                fields['password_confirm'] = fields['new_password']
                self.assertEqual(client.post('/api/auth/change-password/', fields).status_code, 200)
                user.refresh_from_db()
                self.assertTrue(user.check_password('AfterTest73!'))
