# URL connections for product listings, categories, photos and saved items.
# The project router in config/urls.py includes these endpoints under its API prefix.
from rest_framework.routers import DefaultRouter
from .views import CategoryViewSet, ListingViewSet, WishlistViewSet

router = DefaultRouter()
router.register('categories', CategoryViewSet, basename='category')
router.register('wishlist', WishlistViewSet, basename='wishlist')
router.register('listings', ListingViewSet, basename='listing')

urlpatterns = router.urls
