# API data conversion for messages exchanged between marketplace users.
# Serializers turn model records into response data and validate incoming fields before saving.
from rest_framework import serializers
from .models import Message


class MessageSerializer(serializers.ModelSerializer):
    def validate_receiver(self, receiver):
        if receiver.pk == self.context['request'].user.pk:
            raise serializers.ValidationError('Choose another user to message.')
        if not receiver.is_active or receiver.is_banned:
            raise serializers.ValidationError('This user cannot receive messages.')
        return receiver

    def validate_content(self, content):
        content = content.strip()
        if not content or len(content) > 10000:
            raise serializers.ValidationError('Enter a message between 1 and 10000 characters.')
        return content

    sender_name = serializers.CharField(source='sender.username', read_only=True)
    receiver_name = serializers.CharField(source='receiver.username', read_only=True)
    listing_title = serializers.CharField(source='listing.title', read_only=True)

    class Meta:
        model = Message
        fields = ['id', 'sender', 'sender_name', 'receiver', 'receiver_name', 'listing', 'listing_title',
                  'content', 'is_read', 'sent_at']
        read_only_fields = ['id', 'sender', 'is_read', 'sent_at']
