# URL connections for in-app notices about marketplace activity.
# The project router in config/urls.py includes these endpoints under its API prefix.
from rest_framework.routers import DefaultRouter
from .views import NotificationViewSet

router = DefaultRouter()
router.register('notifications', NotificationViewSet, basename='notification')

urlpatterns = router.urls
