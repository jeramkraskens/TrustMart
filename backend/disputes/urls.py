# URL connections for buyer complaints and moderator decisions about orders.
# The project router in config/urls.py includes these endpoints under its API prefix.
from rest_framework.routers import DefaultRouter
from .views import DisputeViewSet

router = DefaultRouter()
router.register('disputes', DisputeViewSet, basename='dispute')

urlpatterns = router.urls
