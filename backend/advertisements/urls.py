# URL connections for promotional offers displayed in the marketplace.
# The project router in config/urls.py includes these endpoints under its API prefix.
from rest_framework.routers import DefaultRouter
from .views import AdvertisementViewSet

router = DefaultRouter()
router.register('advertisements', AdvertisementViewSet, basename='advertisement')

urlpatterns = router.urls
