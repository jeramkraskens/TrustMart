"""Private invalidation events; message contents are fetched through the authorized REST API."""
import asyncio
import time

from channels.db import database_sync_to_async
from channels.generic.websocket import AsyncJsonWebsocketConsumer
from django.conf import settings
from accounts.authentication import MarketplaceJWTAuthentication


@database_sync_to_async
def authenticate(token):
    auth = MarketplaceJWTAuthentication()
    validated = auth.get_validated_token(token)
    return auth.get_user(validated).pk, int(validated['exp'])


class MessageConsumer(AsyncJsonWebsocketConsumer):
    group_prefix = 'messages'
    async def connect(self):
        self.group = None
        self.token = None
        self.timeout = None
        origin = dict(self.scope['headers']).get(b'origin')
        # Native clients have no Origin. Browser clients must use an allowed website.
        if origin and origin.decode() not in settings.CORS_ALLOWED_ORIGINS:
            await self.close(code=4403)
            return
        await self.accept()
        self.timeout = asyncio.create_task(self.expire(10))

    async def expire(self, seconds):
        await asyncio.sleep(max(0, seconds))
        await self.close(code=4401)

    async def receive_json(self, content, **kwargs):
        if self.group or not isinstance(content, dict):
            await self.close(code=4400)
            return
        token = content.get('token')
        if not isinstance(token, str) or len(token) > 8192:
            await self.close(code=4401)
            return
        try:
            user_id, expires = await authenticate(token)
        except Exception:
            await self.close(code=4401)
            return
        self.token = token
        self.group = f'{self.group_prefix}.user.{user_id}'
        await self.channel_layer.group_add(self.group, self.channel_name)
        self.timeout.cancel()
        self.timeout = asyncio.create_task(self.expire(expires - time.time()))
        # Subscribe before ready; clients fetch history on ready to recover gaps.
        await self.send_json({'type': 'ready'})

    async def messages_changed(self, event):
        try:
            await authenticate(self.token)
        except Exception:
            await self.close(code=4401)
            return
        await self.send_json({'type': 'messages.changed'})

    async def disconnect(self, code):
        if self.timeout:
            self.timeout.cancel()
        if self.group:
            await self.channel_layer.group_discard(self.group, self.channel_name)
