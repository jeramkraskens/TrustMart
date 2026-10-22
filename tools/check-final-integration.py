"""Read-only local API/schema/media check for the website and both Android apps.

Uses existing users without printing credentials or personal record contents.
This does not test network reachability from a physical device.
"""
import os
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'backend'))
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings')
import django
django.setup()
from django.contrib.auth import get_user_model
from django.urls import resolve
from rest_framework.test import APIRequestFactory, force_authenticate
from listings.models import Listing, ListingImage
from transactions.models import Transaction
from verification.models import VerificationRequest
from advertisements.models import Advertisement

factory = APIRequestFactory()
checks = 0
def get(path, user=None):
    global checks
    request = factory.get(path, HTTP_HOST='localhost')
    if user:
        force_authenticate(request, user=user)
    match = resolve(path.split('?')[0])
    response = match.func(request, **match.kwargs)
    assert response.status_code == 200, (path, response.status_code)
    checks += 1
    return response.data

locations = get('/api/listings/locations/')
assert len(locations) == 25 and all(isinstance(towns, list) for towns in locations.values())
assert isinstance(get('/api/listings/suggestions/?q=phone'), list)
get('/api/advertisements/?active_only=true')
for role in ('buyer', 'seller', 'moderator'):
    user = get_user_model().objects.filter(role=role, is_active=True, is_banned=False).first()
    assert user is not None, 'No active account for role: ' + role
    me = get('/api/auth/me/', user)
    assert {'district', 'phone', 'nic_number', 'nic_suffix'} <= set(me)
    listings = get('/api/listings/?mine=true' if role == 'seller' else '/api/listings/', user)['results']
    for item in listings:
        assert {'district', 'city', 'contact_phone', 'whatsapp_enabled', 'can_manage'} <= set(item)
    for path in ('/api/messages/', '/api/notifications/', '/api/verifications/', '/api/disputes/'):
        data = get(path, user)
        if path in ('/api/verifications/', '/api/disputes/'):
            assert all('listing_detail' in row for row in data['results'])
    if role != 'moderator':
        get('/api/transactions/', user)

missing = 0
for model, field in ((ListingImage, 'image'), (VerificationRequest, 'evidence'), (Advertisement, 'image')):
    for obj in model.objects.all():
        value = getattr(obj, field)
        if value and not value.storage.exists(value.name):
            missing += 1
print('Read-only API checks passed:', checks)
print('Districts:', len(locations))
print('Database counts:', {'accounts': get_user_model().objects.count(), 'listings': Listing.objects.count(), 'orders': Transaction.objects.count()})
print('Missing referenced media files:', missing)
if missing:
    raise SystemExit('Some database media references need original files; no records were changed.')
