"""
ASGI config for config project.

It exposes the ASGI callable as a module-level variable named ``application``.

For more information on this file, see
https://docs.djangoproject.com/en/6.1/howto/deployment/asgi/
"""

import os

from django.core.asgi import get_asgi_application

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings')

django_application = get_asgi_application()

from channels.routing import ProtocolTypeRouter, URLRouter
from django.urls import path
from messaging.consumers import MessageConsumer
from notifications.consumers import NotificationConsumer

application = ProtocolTypeRouter({
    'http': django_application,
    'websocket': URLRouter([
        path('ws/messages/', MessageConsumer.as_asgi()),
        path('ws/notifications/', NotificationConsumer.as_asgi()),
    ]),
})
