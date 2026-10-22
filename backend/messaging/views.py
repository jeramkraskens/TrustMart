# HTTP request handlers for messages exchanged between marketplace users.
# Read the permission settings, then the query filters, then the action methods to follow a request.
from django.db.models import Q
from django.db import transaction
from rest_framework import viewsets, permissions
from .models import Message
from .serializers import MessageSerializer
from notifications.utils import notify
from .realtime import publish_message


class MessageViewSet(viewsets.ModelViewSet):
    http_method_names = ['get', 'post', 'head', 'options']
    serializer_class = MessageSerializer
    permission_classes = [permissions.IsAuthenticated]

    # Section: Private conversations — return messages involving the current user.
    def get_queryset(self):
        user = self.request.user
        qs = Message.objects.filter(Q(sender=user) | Q(receiver=user)).select_related(
            'sender', 'receiver', 'listing',
        ).order_by('sent_at', 'id')
        with_user = self.request.query_params.get('with')
        if with_user:
            qs = qs.filter(Q(sender_id=with_user) | Q(receiver_id=with_user))
        return qs

    @transaction.atomic
    def perform_create(self, serializer):
        msg = serializer.save(sender=self.request.user)
        transaction.on_commit(lambda: publish_message(msg.sender_id, msg.receiver_id))
        notify(msg.receiver, 'new_message', f'New message from {msg.sender.username}',
               link=f'/messages?with={msg.sender_id}')
