# Backend entry point for URL matching.
# Django checks these routes to choose which app handles each incoming HTTP request.
from django.conf import settings
from django.conf.urls.static import static
from django.contrib import admin
from django.urls import path, include
from .health import health

# Section: API routes — connect each marketplace module to its URL prefix.
urlpatterns = [
    path('api/health/', health),
    path('admin/', admin.site.urls),
    path('api/auth/', include('accounts.urls')),
    path('api/', include('listings.urls')),
    path('api/', include('verification.urls')),
    path('api/', include('transactions.urls')),
    path('api/', include('disputes.urls')),
    path('api/', include('reviews.urls')),
    path('api/', include('messaging.urls')),
    path('api/', include('notifications.urls')),
    path('api/', include('recommendations.urls')),
    path('api/', include('advertisements.urls')),
]

if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
