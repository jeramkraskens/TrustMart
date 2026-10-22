import logging
from asgiref.sync import async_to_sync
from channels.layers import get_channel_layer

logger = logging.getLogger(__name__)


def publish_message(sender_id, receiver_id):
    # A transport outage must not turn a saved message into an apparent failed send.
    try:
        layer = get_channel_layer()
        for user_id in {sender_id, receiver_id}:
            async_to_sync(layer.group_send)(
                f'messages.user.{user_id}', {'type': 'messages.changed'},
            )
    except Exception:
        logger.exception('Live message update failed; clients can recover from REST history.')
