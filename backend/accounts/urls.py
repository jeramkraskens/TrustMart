# URL connections for user accounts, roles and profile details.
# The project router in config/urls.py includes these endpoints under its API prefix.
from django.urls import path
from rest_framework_simplejwt.views import TokenRefreshView
from rest_framework.routers import DefaultRouter
from .views import (
    RegisterView, MyTokenObtainPairView, MeView, ChangePasswordView, UserManagementViewSet
)
from .analytics import AdminAnalyticsView, AdminRevenueView

router = DefaultRouter()
router.register('users', UserManagementViewSet, basename='user-management')

urlpatterns = [
    path('register/', RegisterView.as_view(), name='register'),
    path('login/', MyTokenObtainPairView.as_view(), name='login'),
    path('login/refresh/', TokenRefreshView.as_view(), name='login-refresh'),
    path('me/', MeView.as_view(), name='me'),
    path('change-password/', ChangePasswordView.as_view(), name='change-password'),
    path('admin-analytics/', AdminAnalyticsView.as_view(), name='admin-analytics'),
    path('admin-revenue/', AdminRevenueView.as_view(), name='admin-revenue'),
] + router.urls
