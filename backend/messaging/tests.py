from channels.db import database_sync_to_async
from channels.testing import WebsocketCommunicator
from django.contrib.auth import get_user_model
from django.test import TransactionTestCase, override_settings
from rest_framework.test import APIClient
from rest_framework_simplejwt.tokens import AccessToken

from config.asgi import application


@override_settings(CHANNEL_LAYERS={'default': {'BACKEND': 'channels.layers.InMemoryChannelLayer'}})
class LiveMessageTests(TransactionTestCase):
    def setUp(self):
        self.users = {
            role: get_user_model().objects.create_user(username=role, role=role)
            for role in ['buyer', 'seller', 'moderator']
        }
        self.tokens = {role: str(AccessToken.for_user(user)) for role, user in self.users.items()}

    async def connect(self, role):
        socket = WebsocketCommunicator(application, '/ws/messages/')
        connected, _ = await socket.connect()
        self.assertTrue(connected)
        await socket.send_json_to({'token': self.tokens[role]})
        self.assertEqual(await socket.receive_json_from(), {'type': 'ready'})
        return socket

    @database_sync_to_async
    def request(self, role, method, path='/api/messages/', data=None):
        client = APIClient()
        client.credentials(HTTP_AUTHORIZATION=f'Bearer {self.tokens[role]}')
        response = getattr(client, method)(path, data=data, format='json')
        return response.status_code, getattr(response, 'data', None)

    async def test_buyer_seller_and_moderator_seller_are_private_and_bidirectional(self):
        sockets = {role: await self.connect(role) for role in self.users}
        try:
            for sender, receiver, excluded in [
                ('buyer', 'seller', 'moderator'),
                ('seller', 'buyer', 'moderator'),
                ('moderator', 'seller', 'buyer'),
                ('seller', 'moderator', 'buyer'),
            ]:
                status, message = await self.request(sender, 'post', data={
                    'receiver': self.users[receiver].pk, 'content': f'{sender} to {receiver}',
                })
                self.assertEqual(status, 201)
                self.assertEqual(message['sender'], self.users[sender].pk)
                for participant in [sender, receiver]:
                    self.assertEqual(await sockets[participant].receive_json_from(), {'type': 'messages.changed'})
                self.assertTrue(await sockets[excluded].receive_nothing(timeout=0.05))
                status, _ = await self.request(excluded, 'get', f"/api/messages/{message['id']}/")
                self.assertEqual(status, 404)
        finally:
            for socket in sockets.values():
                await socket.disconnect()

    async def test_reconnect_recovers_offline_history(self):
        socket = await self.connect('seller')
        await socket.disconnect()
        status, message = await self.request('buyer', 'post', data={
            'receiver': self.users['seller'].pk, 'content': 'Saved while offline',
        })
        self.assertEqual(status, 201)
        socket = await self.connect('seller')
        try:
            status, history = await self.request('seller', 'get')
            self.assertEqual(status, 200)
            self.assertIn(message['id'], [m['id'] for m in history['results']])
        finally:
            await socket.disconnect()

    async def test_rejects_invalid_expired_and_banned_tokens(self):
        expired = AccessToken.for_user(self.users['buyer'])
        expired['exp'] = 1
        await database_sync_to_async(get_user_model().objects.filter(pk=self.users['buyer'].pk).update)(is_banned=True)
        for token in ['invalid', str(expired), self.tokens['buyer']]:
            socket = WebsocketCommunicator(application, '/ws/messages/')
            await socket.connect()
            await socket.send_json_to({'token': token})
            self.assertEqual((await socket.receive_output())['code'], 4401)
            await socket.disconnect()

    async def test_rejects_untrusted_browser_origin(self):
        socket = WebsocketCommunicator(application, '/ws/messages/', headers=[(b'origin', b'https://untrusted.example')])
        connected, code = await socket.connect()
        self.assertFalse(connected)
        self.assertEqual(code, 4403)
        await socket.disconnect()

    async def test_receiver_cannot_edit_or_delete_sender_message(self):
        _, message = await self.request('buyer', 'post', data={
            'receiver': self.users['seller'].pk, 'content': 'Original',
        })
        for method in ['patch', 'delete']:
            status, _ = await self.request('seller', method, f"/api/messages/{message['id']}/", {'content': 'Changed'})
            self.assertEqual(status, 405)

    async def test_rejects_self_and_blank_messages(self):
        for payload in [
            {'receiver': self.users['buyer'].pk, 'content': 'Self'},
            {'receiver': self.users['seller'].pk, 'content': '   '},
        ]:
            status, _ = await self.request('buyer', 'post', data=payload)
            self.assertEqual(status, 400)
