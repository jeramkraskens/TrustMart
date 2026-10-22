from messaging.consumers import MessageConsumer, authenticate


class NotificationConsumer(MessageConsumer):
    group_prefix = 'notifications'

    async def notifications_changed(self, event):
        try:
            await authenticate(self.token)
        except Exception:
            await self.close(code=4401)
            return
        await self.send_json({'type': 'notifications.changed', 'notification': event.get('notification')})
