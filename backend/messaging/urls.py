# URL connections for messages exchanged between marketplace users.
# The project router in config/urls.py includes these endpoints under its API prefix.
from rest_framework.routers import DefaultRouter
from .views import MessageViewSet

router = DefaultRouter()
router.register('messages', MessageViewSet, basename='message')

urlpatterns = router.urls
