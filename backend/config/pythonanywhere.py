"""Free demo hosting: one ASGI process, local persistent SQLite and uploads."""
import os

os.environ.setdefault('DEBUG', 'False')
os.environ.setdefault('USE_POSTGRES', 'False')
os.environ.setdefault('USE_DAPHNE', 'False')
os.environ.setdefault('SINGLE_PROCESS_CHANNELS', 'True')

from .settings import *  # noqa: F403,E402
from django.core.exceptions import ImproperlyConfigured

if DEBUG or SECRET_KEY == 'django-insecure-change-this-in-production':
    raise ImproperlyConfigured('Configure DEBUG=False and a random SECRET_KEY.')
if len(SECRET_KEY) < 43:
    raise ImproperlyConfigured('SECRET_KEY must contain at least 256 bits of random entropy.')

ROOT_URLCONF = 'config.hosted_urls'
MIDDLEWARE.insert(1, 'config.hosted_files.FrontendStaticMiddleware')
STATIC_URL = '/static/'
STATIC_ROOT = BASE_DIR / 'staticfiles'
FRONTEND_DIST = BASE_DIR.parent / 'deploy' / 'web'
STORAGES = {
    'default': {'BACKEND': 'config.hosted_files.SignedMediaStorage'},
    'staticfiles': {'BACKEND': 'whitenoise.storage.CompressedManifestStaticFilesStorage'},
}
DATABASES['default']['OPTIONS'] = {'timeout': 20, 'transaction_mode': 'IMMEDIATE'}
CSRF_TRUSTED_ORIGINS = [f'https://{host}' for host in ALLOWED_HOSTS]
CORS_ALLOWED_ORIGINS = CSRF_TRUSTED_ORIGINS
SESSION_COOKIE_SECURE = True
CSRF_COOKIE_SECURE = True
SECURE_PROXY_SSL_HEADER = ('HTTP_X_FORWARDED_PROTO', 'https')
SECURE_SSL_REDIRECT = True
SECURE_HSTS_SECONDS = 3600
SECURE_CONTENT_TYPE_NOSNIFF = True
FILE_UPLOAD_MAX_MEMORY_SIZE = 2 * 1024 * 1024
DATA_UPLOAD_MAX_MEMORY_SIZE = 12 * 1024 * 1024
