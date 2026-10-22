"""Production settings for Render with external PostgreSQL and S3 storage."""
from .settings import *  # noqa: F403
import dj_database_url
from django.core.exceptions import ImproperlyConfigured

if DEBUG:
    raise ImproperlyConfigured('Production requires DEBUG=False.')
if SECRET_KEY == 'django-insecure-change-this-in-production' or len(SECRET_KEY) < 43:
    raise ImproperlyConfigured('Set a randomly generated SECRET_KEY with at least 256 bits of entropy.')

render_host = config('RENDER_EXTERNAL_HOSTNAME', default='')
if render_host and render_host not in ALLOWED_HOSTS:
    ALLOWED_HOSTS.append(render_host)

DATABASES = {'default': dj_database_url.parse(
    config('DATABASE_URL'), conn_max_age=0, ssl_require=True,
)}
DATABASES['default']['DISABLE_SERVER_SIDE_CURSORS'] = True

MIDDLEWARE.insert(1, 'whitenoise.middleware.WhiteNoiseMiddleware')
STATIC_ROOT = BASE_DIR / 'staticfiles'
STATIC_URL = '/static/'
STORAGES = {
    'staticfiles': {
        'BACKEND': 'whitenoise.storage.CompressedManifestStaticFilesStorage',
    },
    'default': {
        'BACKEND': 'storages.backends.s3.S3Storage',
        'OPTIONS': {
            'access_key': config('S3_ACCESS_KEY_ID'),
            'secret_key': config('S3_SECRET_ACCESS_KEY'),
            'bucket_name': config('S3_BUCKET_NAME'),
            'endpoint_url': config('S3_ENDPOINT_URL'),
            'region_name': config('S3_REGION_NAME'),
            'signature_version': 's3v4',
            'addressing_style': 'path',
            'default_acl': None,
            'file_overwrite': False,
            # Private bucket: API permissions control who receives temporary URLs.
            'querystring_auth': True,
            'querystring_expire': 3600,
        },
    },
}

CSRF_TRUSTED_ORIGINS = config('CSRF_TRUSTED_ORIGINS', default='', cast=Csv())
if render_host:
    CSRF_TRUSTED_ORIGINS.append(f'https://{render_host}')
SECURE_PROXY_SSL_HEADER = ('HTTP_X_FORWARDED_PROTO', 'https')
SECURE_SSL_REDIRECT = True
SESSION_COOKIE_SECURE = True
CSRF_COOKIE_SECURE = True
SECURE_HSTS_SECONDS = 3600
SECURE_CONTENT_TYPE_NOSNIFF = True
