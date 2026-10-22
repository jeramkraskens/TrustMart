# URL connections for ratings and feedback associated with purchases.
# The project router in config/urls.py includes these endpoints under its API prefix.
from rest_framework.routers import DefaultRouter
from .views import ReviewViewSet

router = DefaultRouter()
router.register('reviews', ReviewViewSet, basename='review')

urlpatterns = router.urls
