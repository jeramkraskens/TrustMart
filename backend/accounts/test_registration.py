from django.contrib.auth import get_user_model
from rest_framework.test import APITestCase

User = get_user_model()


class PasswordConfirmationTests(APITestCase):
    def payload(self, **changes):
        return {'username': 'new_buyer', 'email': 'buyer@example.com', 'role': 'buyer',
                'first_name': 'Test', 'last_name': 'Buyer', 'phone': '0771234567', 'district': 'Colombo',
                'password': 'ExamplePass!2026', 'password_confirm': 'ExamplePass!2026', **changes}

    def test_matching_password_creates_account_and_is_never_returned(self):
        response = self.client.post('/api/auth/register/', self.payload(), format='json')
        self.assertEqual(response.status_code, 201, response.data)
        self.assertNotIn('password', response.data)
        self.assertNotIn('password_confirm', response.data)

    def test_required_fields_and_district(self):
        for key in ('first_name', 'last_name', 'phone', 'email', 'district', 'username'):
            for value in ('', None):
                data = self.payload(**{key: value})
                if value is None:
                    data.pop(key)
                response = self.client.post('/api/auth/register/', data, format='json')
                self.assertEqual(response.status_code, 400, response.data)
                self.assertIn(key, response.data)
        self.assertEqual(self.client.post('/api/auth/register/', self.payload(district='Unknown'), format='json').status_code, 400)

    def test_phone_must_be_exactly_ten_ascii_digits(self):
        for phone in ('+94771234567', '077 1234567', '077123456', '07712345678', '077123456a', '0771234.67', ' 0771234567', '０７７１２３４５６７'):
            response = self.client.post('/api/auth/register/', self.payload(phone=phone), format='json')
            self.assertEqual(response.status_code, 400, phone)
            self.assertIn('phone', response.data)

    def test_password_requires_all_three_character_types(self):
        for password in ('onlylowercase12', 'ONLYUPPERCASE12', 'NoDigitsHere', 'Short1A'):
            response = self.client.post('/api/auth/register/', self.payload(password=password, password_confirm=password), format='json')
            self.assertEqual(response.status_code, 400, password)
            self.assertIn('password', response.data)

    def test_seller_nic_required_with_optional_v_or_x_and_no_photo(self):
        self.assertEqual(self.client.post('/api/auth/register/', self.payload(role='seller'), format='json').status_code, 400)
        for suffix in ('V', 'X'):
            response = self.client.post('/api/auth/register/', self.payload(username=f'seller_{suffix}', role='seller', nic_number='912345678', nic_suffix=suffix), format='json')
            self.assertEqual(response.status_code, 201, response.data)
            self.assertNotIn('nic_photo', response.data)
        for number, suffix in (('', ''), ('912345678V', 'V'), ('912345678', 'A'), ('123 456', ''), ('123.45', ''), ('123\n', '')):
            response = self.client.post('/api/auth/register/', self.payload(role='seller', nic_number=number, nic_suffix=suffix), format='json')
            self.assertEqual(response.status_code, 400, response.data)

    def test_seller_nic_accepts_any_digit_count_with_blank_or_omitted_suffix(self):
        for index, number in enumerate(('1', '123', '001234567', '200012345678', '0' * 100)):
            data = self.payload(username=f'flexible_nic_{index}', role='seller', nic_number=number)
            if index % 2:
                data['nic_suffix'] = ''
            response = self.client.post('/api/auth/register/', data, format='json')
            self.assertEqual(response.status_code, 201, response.data)
            user = User.objects.get(username=data['username'])
            self.assertEqual(user.nic_number, number)
            self.assertEqual(user.nic_suffix, '')
            self.assertEqual(response.data['nic_suffix'], '')

    def test_seller_profile_can_replace_nic_and_clear_existing_suffix(self):
        self.client.post('/api/auth/register/', self.payload(role='seller', nic_number='912345678', nic_suffix='V'), format='json')
        user = User.objects.get(username='new_buyer')
        self.client.force_authenticate(user)
        response = self.client.patch('/api/auth/me/', {'nic_number': '200012345678', 'nic_suffix': ''}, format='json')
        self.assertEqual(response.status_code, 200, response.data)
        user.refresh_from_db()
        self.assertEqual(user.nic_number, '200012345678')
        self.assertEqual(user.nic_suffix, '')
        response = self.client.patch('/api/auth/me/', {'nic_number': '00123'}, format='json')
        self.assertEqual(response.status_code, 200, response.data)
        self.assertEqual(response.data['nic_number'], '00123')
        self.assertEqual(response.data['nic_suffix'], '')

    def test_admin_can_create_seller_without_nic_suffix(self):
        self.client.force_authenticate(User.objects.create_user(username='nic_admin', role='admin'))
        response = self.client.post('/api/auth/users/', self.payload(role='seller', nic_number='200012345678'), format='json')
        self.assertEqual(response.status_code, 201, response.data)
        self.assertEqual(response.data['nic_suffix'], '')

    def test_buyer_profile_and_password_change(self):
        self.client.post('/api/auth/register/', self.payload(), format='json')
        user = User.objects.get(username='new_buyer')
        self.client.force_authenticate(user)
        response = self.client.patch('/api/auth/me/', {'first_name': 'Changed', 'last_name': 'Name', 'phone': '0712345678', 'email': 'changed@example.com', 'district': 'Kandy'}, format='json')
        self.assertEqual(response.status_code, 200, response.data)
        self.assertEqual(response.data['district'], 'Kandy')
        self.assertEqual(self.client.patch('/api/auth/me/', {'phone': '+94712345678'}, format='json').status_code, 400)
        for changes in ({'old_password': 'wrong'}, {'password_confirm': 'wrong'}, {'new_password': 'weak', 'password_confirm': 'weak'}):
            data = {'old_password': 'ExamplePass!2026', 'new_password': 'BetterPass2027', 'password_confirm': 'BetterPass2027', **changes}
            self.assertEqual(self.client.post('/api/auth/change-password/', data, format='json').status_code, 400)
            user.refresh_from_db()
            self.assertTrue(user.check_password('ExamplePass!2026'))
        response = self.client.post('/api/auth/change-password/', {'old_password': 'ExamplePass!2026', 'new_password': 'BetterPass2027', 'password_confirm': 'BetterPass2027'}, format='json')
        self.assertEqual(response.status_code, 200, response.data)
        user.refresh_from_db()
        self.assertTrue(user.check_password('BetterPass2027'))

    def test_seller_can_update_identity_but_cannot_change_role(self):
        data = self.payload(role='seller', nic_number='912345678', nic_suffix='V')
        self.client.post('/api/auth/register/', data, format='json')
        self.client.force_authenticate(User.objects.get(username='new_buyer'))
        response = self.client.patch('/api/auth/me/', {'nic_number': '923456789', 'nic_suffix': 'X', 'district': 'Galle', 'role': 'admin'}, format='json')
        self.assertEqual(response.status_code, 200, response.data)
        self.assertEqual(response.data['nic_suffix'], 'X')
        self.assertEqual(response.data['role'], 'seller')
        self.assertTrue(User.objects.get(username='new_buyer').check_password('ExamplePass!2026'))

    def test_missing_blank_and_mismatching_confirmation_do_not_create_account(self):
        for value in [None, '', 'DifferentPass!2026']:
            data = self.payload(password_confirm=value)
            if value is None:
                data.pop('password_confirm')
            response = self.client.post('/api/auth/register/', data, format='json')
            self.assertEqual(response.status_code, 400, response.data)
            self.assertIn('password_confirm', response.data)
            self.assertFalse(User.objects.filter(username='new_buyer').exists())

    def test_comparison_is_exact_and_password_strength_still_applies(self):
        response = self.client.post('/api/auth/register/', self.payload(password_confirm='ExamplePass!2026 '), format='json')
        self.assertEqual(response.status_code, 400)
        response = self.client.post('/api/auth/register/', self.payload(password='123', password_confirm='123'), format='json')
        self.assertEqual(response.status_code, 400)
        self.assertIn('password', response.data)

    def test_admin_create_also_requires_matching_passwords(self):
        admin = User.objects.create_user(username='confirm_admin', role='admin')
        self.client.force_authenticate(admin)
        response = self.client.post('/api/auth/users/', self.payload(password_confirm='WrongPass!2026'), format='json')
        self.assertEqual(response.status_code, 400)
        self.assertFalse(User.objects.filter(username='new_buyer').exists())
        response = self.client.post('/api/auth/users/', self.payload(), format='json')
        self.assertEqual(response.status_code, 201, response.data)
        self.assertNotIn('password_confirm', response.data)
