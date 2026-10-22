from unittest.mock import patch

from channels.db import database_sync_to_async
from channels.testing import WebsocketCommunicator
from django.contrib.auth import get_user_model
from django.db import transaction
from django.test import TestCase, TransactionTestCase, override_settings
from rest_framework.test import APIClient
from rest_framework_simplejwt.tokens import AccessToken

from config.asgi import application
from listings.models import Listing
from verification.models import VerificationRequest
from transactions.models import Transaction
from .models import Notification
from .utils import notify, notify_staff


class NotificationTests(TestCase):
    def setUp(self):
        self.users = {role: get_user_model().objects.create_user(username=role, role=role)
                      for role in ['buyer', 'seller', 'moderator', 'admin']}
        self.client = APIClient()
        self.listing = Listing.objects.create(seller=self.users['seller'], title='Phone', price='100.00', status='verified')

    def login(self, role):
        self.client.force_authenticate(self.users[role])

    def test_private_history_read_counts_and_write_protection_for_every_role(self):
        for role, user in self.users.items():
            own = notify(user, 'test', 'Private notice')
            other = notify(next(u for u in self.users.values() if u.pk != user.pk), 'test', 'Other notice')
            self.login(role)
            rows = self.client.get('/api/notifications/').data['results']
            self.assertIn(own.pk, [row['id'] for row in rows])
            self.assertNotIn(other.pk, [row['id'] for row in rows])
            self.assertEqual(self.client.post(f'/api/notifications/{other.pk}/mark_read/').status_code, 404)
            self.assertEqual(self.client.post('/api/notifications/', {'user': user.pk, 'content': 'Forged'}).status_code, 405)
            self.assertEqual(self.client.patch(f'/api/notifications/{own.pk}/', {'content': 'Edited'}).status_code, 405)
            self.assertEqual(self.client.delete(f'/api/notifications/{own.pk}/').status_code, 405)
            self.assertEqual(self.client.post(f'/api/notifications/{own.pk}/mark_read/').status_code, 200)
            self.assertEqual(self.client.post('/api/notifications/mark_all_read/').status_code, 200)
            self.assertEqual(self.client.get('/api/notifications/unread_count/').data['count'], 0)
            other.refresh_from_db()
            self.assertFalse(other.is_read)

    def test_commit_publishes_once_and_rollback_publishes_nothing(self):
        with patch('notifications.utils.publish_notifications') as publish:
            with self.captureOnCommitCallbacks(execute=True):
                with transaction.atomic():
                    notification = notify(self.users['buyer'], 'test', 'Committed', link='/buyer')
                publish.assert_not_called()
            publish.assert_called_once()
            self.assertEqual(publish.call_args.args[1]['id'], notification.pk)
            publish.reset_mock()
            with self.captureOnCommitCallbacks(execute=True):
                try:
                    with transaction.atomic():
                        notify(self.users['seller'], 'test', 'Rolled back')
                        raise ValueError('rollback')
                except ValueError:
                    pass
            publish.assert_not_called()
            self.assertFalse(Notification.objects.filter(content='Rolled back').exists())

    def test_verification_submissions_reach_seller_and_staff_once(self):
        # The listing serializer also creates VerificationRequest directly; signals cover both paths.
        VerificationRequest.objects.create(listing=self.listing)
        self.assertEqual(Notification.objects.filter(type='verification_submitted', user=self.users['seller']).count(), 1)
        self.assertEqual(set(Notification.objects.filter(type='verification_pending').values_list('user__role', flat=True)), {'moderator', 'admin'})
        self.assertFalse(Notification.objects.filter(user=self.users['buyer']).exists())

    def test_staff_broadcast_excludes_banned_and_inactive_accounts(self):
        self.users['moderator'].is_banned = True
        self.users['moderator'].save()
        notify_staff('test', 'Review needed')
        self.assertEqual(list(Notification.objects.values_list('user__role', flat=True)), ['admin'])

    def test_purchase_and_delivery_notify_correct_roles_without_duplicates(self):
        self.login('buyer')
        response = self.client.post('/api/transactions/', {'listing': self.listing.pk}, format='json')
        self.assertEqual(response.status_code, 201, response.data)
        txn_id = response.data['id']
        self.assertTrue(Notification.objects.filter(type='new_sale', user=self.users['seller']).exists())
        self.assertTrue(Notification.objects.filter(type='order_created', user=self.users['buyer']).exists())
        self.assertTrue(Notification.objects.filter(type='order_created', user=self.users['admin']).exists())
        self.assertFalse(Notification.objects.filter(user=self.users['moderator']).exists())
        url = f'/api/transactions/{txn_id}/confirm_delivery/'
        self.assertEqual(self.client.post(url).status_code, 200)
        count = Notification.objects.count()
        self.assertEqual(self.client.post(url).status_code, 200)
        self.assertEqual(Notification.objects.count(), count)

    def test_dispute_creation_notifies_participants_and_staff(self):
        txn = Transaction.objects.create(buyer=self.users['buyer'], listing=self.listing, amount='100')
        self.login('buyer')
        response = self.client.post('/api/disputes/', {'transaction': txn.pk, 'reason': 'Wrong item'}, format='json')
        self.assertEqual(response.status_code, 201, response.data)
        self.assertEqual(set(Notification.objects.filter(type='dispute_opened').values_list('user__role', flat=True)), set(self.users))

    def test_verification_decision_notifies_seller_and_admin(self):
        self.listing.status = Listing.Status.PENDING
        self.listing.save()
        vr = VerificationRequest.objects.create(listing=self.listing)
        Notification.objects.all().delete()
        self.login('moderator')
        self.assertEqual(self.client.post(f'/api/verifications/{vr.pk}/approve/').status_code, 200)
        self.assertTrue(Notification.objects.filter(type='verification_approved', user=self.users['seller']).exists())
        self.assertTrue(Notification.objects.filter(type='verification_reviewed', user=self.users['admin']).exists())
        count = Notification.objects.count()
        self.assertEqual(self.client.post(f'/api/verifications/{vr.pk}/approve/').status_code, 400)
        self.assertEqual(Notification.objects.count(), count)

    def test_account_changes_notify_user_and_other_admin(self):
        self.login('admin')
        response = self.client.patch(f"/api/auth/users/{self.users['buyer'].pk}/", {'is_banned': True}, format='json')
        self.assertEqual(response.status_code, 200, response.data)
        self.assertTrue(Notification.objects.filter(user=self.users['buyer'], type='account_updated').exists())

    def test_unauthenticated_history_is_rejected(self):
        self.assertEqual(self.client.get('/api/notifications/').status_code, 401)


@override_settings(CHANNEL_LAYERS={'default': {'BACKEND': 'channels.layers.InMemoryChannelLayer'}})
class LiveNotificationTests(TransactionTestCase):
    def setUp(self):
        self.buyer = get_user_model().objects.create_user(username='buyer')
        self.seller = get_user_model().objects.create_user(username='seller', role='seller')

    async def connect(self, user):
        socket = WebsocketCommunicator(application, '/ws/notifications/')
        self.assertTrue((await socket.connect())[0])
        await socket.send_json_to({'token': str(AccessToken.for_user(user))})
        self.assertEqual(await socket.receive_json_from(), {'type': 'ready'})
        return socket

    async def test_only_owner_receives_live_alert_and_read_sync(self):
        buyer = await self.connect(self.buyer)
        seller = await self.connect(self.seller)
        try:
            notification = await database_sync_to_async(notify)(self.buyer, 'new_message', 'Private update', '/messages')
            event = await buyer.receive_json_from()
            self.assertEqual(event['type'], 'notifications.changed')
            self.assertEqual(event['notification']['id'], notification.pk)
            self.assertTrue(await seller.receive_nothing(timeout=0.05))

            @database_sync_to_async
            def read():
                client = APIClient()
                client.force_authenticate(self.buyer)
                return client.post(f'/api/notifications/{notification.pk}/mark_read/').status_code

            self.assertEqual(await read(), 200)
            self.assertEqual(await buyer.receive_json_from(), {'type': 'notifications.changed', 'notification': None})
            self.assertTrue(await seller.receive_nothing(timeout=0.05))
        finally:
            await buyer.disconnect()
            await seller.disconnect()

    async def test_rejects_invalid_websocket_credentials(self):
        socket = WebsocketCommunicator(application, '/ws/notifications/')
        await socket.connect()
        await socket.send_json_to({'token': 'invalid'})
        self.assertEqual((await socket.receive_output())['code'], 4401)
        await socket.disconnect()
