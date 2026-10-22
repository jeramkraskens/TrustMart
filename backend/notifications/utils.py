# Shared helper for storing an in-app notification after a marketplace event.
def notify(user, type_, content, link=''):
    """Persist an in-app notice and publish only after its transaction commits."""
    from django.db import transaction
    from .models import Notification
    from .serializers import NotificationSerializer
    notification = Notification.objects.create(user=user, type=type_, content=content, link=link)
    payload = dict(NotificationSerializer(notification).data)
    transaction.on_commit(lambda: publish_notifications(user.pk, payload))
    return notification


def publish_notifications(user_id, notification=None):
    import logging
    from asgiref.sync import async_to_sync
    from channels.layers import get_channel_layer
    try:
        async_to_sync(get_channel_layer().group_send)(
            f'notifications.user.{user_id}',
            {'type': 'notifications.changed', 'notification': notification},
        )
    except Exception:
        logging.getLogger(__name__).exception('Notification live update failed; saved history remains available.')


def notify_staff(type_, content, *, roles=('admin', 'moderator'), exclude=None, link=''):
    from django.contrib.auth import get_user_model
    from django.db.models import Q
    users = get_user_model().objects.filter(is_active=True, is_banned=False).filter(
        Q(role__in=roles) | Q(is_superuser=True),
    )
    if exclude:
        users = users.exclude(pk=exclude)
    for user in users.iterator():
        notify(user, type_, content, link=link)
