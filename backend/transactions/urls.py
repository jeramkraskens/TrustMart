# URL connections for orders and simulated escrow payment states.
# The project router in config/urls.py includes these endpoints under its API prefix.
from rest_framework.routers import DefaultRouter
from .views import TransactionViewSet

router = DefaultRouter()
router.register('transactions', TransactionViewSet, basename='transaction')

urlpatterns = router.urls
