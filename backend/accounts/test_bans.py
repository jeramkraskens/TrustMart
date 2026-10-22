from django.contrib.auth import get_user_model
from rest_framework.test import APITestCase, APIClient

User = get_user_model()


class BanWorkflowTests(APITestCase):
    def setUp(self):
        self.admin = User.objects.create_user(username='ban_admin', role='admin')
        self.member = User.objects.create_user(username='ban_member', password='ExamplePass!123', role='seller')
        self.client.force_authenticate(self.admin)
        self.member_client = APIClient()
        self.credentials = {'username': self.member.username, 'password': 'ExamplePass!123'}
        self.tokens = self.member_client.post('/api/auth/login/', self.credentials).data

    def set_ban(self, value):
        return self.client.patch(f'/api/auth/users/{self.member.pk}/', {'is_banned': value}, format='json')

    def test_ban_blocks_login_and_unban_restores_login(self):
        response = self.set_ban(True)
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.data['is_banned'])
        self.member.refresh_from_db()
        self.assertTrue(self.member.is_banned)
        self.assertEqual(self.member_client.post('/api/auth/login/', self.credentials).status_code, 401)
        self.assertEqual(self.set_ban(False).status_code, 200)
        self.assertEqual(self.member_client.post('/api/auth/login/', self.credentials).status_code, 200)

    def test_ban_blocks_previously_issued_access_and_refresh_tokens(self):
        self.member_client.credentials(HTTP_AUTHORIZATION=f"Bearer {self.tokens['access']}")
        self.assertEqual(self.member_client.get('/api/auth/me/').status_code, 200)
        self.set_ban(True)
        self.assertEqual(self.member_client.get('/api/auth/me/').status_code, 401)
        self.member_client.credentials()
        self.assertEqual(self.member_client.post('/api/auth/login/refresh/', {'refresh': self.tokens['refresh']}).status_code, 401)

    def test_admin_cannot_ban_self(self):
        response = self.client.patch(f'/api/auth/users/{self.admin.pk}/', {'is_banned': True}, format='json')
        self.assertEqual(response.status_code, 400)
        self.admin.refresh_from_db()
        self.assertFalse(self.admin.is_banned)

    def test_non_admin_cannot_manage_bans(self):
        self.client.force_authenticate(self.member)
        self.assertEqual(self.set_ban(True).status_code, 403)

    def test_profile_cannot_set_ban_status(self):
        self.member_client.credentials(HTTP_AUTHORIZATION=f"Bearer {self.tokens['access']}")
        self.assertEqual(self.member_client.patch('/api/auth/me/', {'is_banned': True}, format='json').status_code, 200)
        self.member.refresh_from_db()
        self.assertFalse(self.member.is_banned)
