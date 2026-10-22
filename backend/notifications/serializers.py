# API data conversion for in-app notices about marketplace activity.
# Serializers turn model records into response data and validate incoming fields before saving.
from rest_framework import serializers
from .models import Notification


class NotificationSerializer(serializers.ModelSerializer):
    class Meta:
        model = Notification
        fields = ['id', 'type', 'content', 'link', 'is_read', 'created_at']
        read_only_fields = fields
