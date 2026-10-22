# URL connections for moderator reviews of listing evidence.
# The project router in config/urls.py includes these endpoints under its API prefix.
from rest_framework.routers import DefaultRouter
from .views import VerificationRequestViewSet

router = DefaultRouter()
router.register('verifications', VerificationRequestViewSet, basename='verification')

urlpatterns = router.urls
