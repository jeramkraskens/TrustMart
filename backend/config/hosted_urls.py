from django.urls import path
from django.contrib import admin
from .urls import urlpatterns as api_patterns
from .hosted_files import frontend, media

urlpatterns = [p for p in api_patterns if str(p.pattern) != 'admin/'] + [
    path('django-admin/', admin.site.urls),
    path('uploads/<str:token>', media),
    path('', frontend),
    path('<path:route>', frontend),
]
