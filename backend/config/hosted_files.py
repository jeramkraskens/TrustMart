"""Static frontend delivery and expiring links for persistent local uploads."""
import mimetypes
from django.conf import settings
from django.core import signing
from django.core.files.storage import FileSystemStorage
from django.http import FileResponse, Http404
from whitenoise.middleware import WhiteNoiseMiddleware


class FrontendStaticMiddleware(WhiteNoiseMiddleware):
    def __init__(self, get_response=None, settings=None):
        super().__init__(get_response, settings)
        from django.conf import settings as django_settings
        self.add_files(str(django_settings.FRONTEND_DIST / 'assets'), prefix='assets/')


class SignedMediaStorage(FileSystemStorage):
    def url(self, name):
        token = signing.dumps(name, salt='trustmart-media')
        return f'/uploads/{token}'


def media(request, token):
    try:
        name = signing.loads(token, salt='trustmart-media', max_age=3600)
        storage = SignedMediaStorage()
        file = storage.open(name, 'rb')
    except (signing.BadSignature, ValueError, OSError):
        raise Http404 from None
    content_type = mimetypes.guess_type(name)[0] or 'application/octet-stream'
    inline = content_type in ('image/jpeg', 'image/png', 'image/webp', 'image/gif')
    response = FileResponse(file, content_type=content_type, as_attachment=not inline)
    response['Cache-Control'] = 'private, no-store'
    response['X-Content-Type-Options'] = 'nosniff'
    return response


def frontend(request, route=''):
    if route.startswith(('api/', 'django-admin/', 'uploads/', 'static/', 'assets/', 'media/', 'ws/')):
        raise Http404
    try:
        return FileResponse((settings.FRONTEND_DIST / 'index.html').open('rb'), content_type='text/html')
    except OSError:
        raise Http404 from None
