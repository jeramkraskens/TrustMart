r"""Read-only smoke check against this workspace's Django backend (no purchases/messages created).

Run from the workspace root with:
    .\backend\.venv\Scripts\python.exe 'TrustMart App/tools/check_backend_contract.py'
"""
import os
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'backend'))
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings')
import django
django.setup()

from django.contrib.auth import get_user_model
from django.urls import resolve
from rest_framework.test import APIRequestFactory, force_authenticate


def main():
    factory = APIRequestFactory()
    # Match the native Gson contracts without printing private message or profile contents.
    required_fields = {
        '/api/auth/me/': {'id', 'username', 'email', 'role', 'first_name', 'last_name', 'phone', 'trust_score'},
        '/api/categories/': {'id', 'name'},
        '/api/listings/': {'id', 'seller', 'seller_name', 'seller_trust_score', 'category', 'category_name',
                           'title', 'description', 'price', 'condition', 'status', 'location', 'images'},
        '/api/wishlist/': {'id', 'listing', 'listing_detail'},
        '/api/transactions/': {'id', 'buyer', 'buyer_name', 'listing', 'listing_title', 'seller_id', 'seller_name',
                               'amount', 'seller_payout', 'escrow_status', 'payment_method', 'delivery_confirmed', 'created_at'},
        '/api/messages/': {'id', 'sender', 'sender_name', 'receiver', 'receiver_name', 'content', 'listing', 'sent_at'},
        '/api/disputes/': {'id', 'transaction', 'listing_title', 'reason', 'status', 'resolution', 'resolution_notes'},
        '/api/reviews/': {'id', 'transaction', 'reviewer', 'reviewer_name', 'reviewee', 'rating', 'comment'},
        '/api/verifications/': {'id', 'listing', 'status', 'notes', 'feedback'},
        '/api/notifications/': {'id', 'content', 'is_read'},
    }
    paths = [
        '/api/auth/me/', '/api/categories/', '/api/listings/?status=verified',
        '/api/listings/?mine=true', '/api/wishlist/', '/api/transactions/',
        '/api/messages/', '/api/disputes/', '/api/reviews/',
        '/api/verifications/', '/api/notifications/',
    ]
    checked = 0
    for role in ('buyer', 'seller'):
        user = get_user_model().objects.filter(role=role, is_active=True).first()
        if user is None:
            raise RuntimeError(f'Create a test {role} account before running this smoke check.')
        for path in paths:
            request = factory.get(path, HTTP_HOST='localhost')
            force_authenticate(request, user=user)
            match = resolve(path.split('?')[0])
            response = match.func(request, **match.kwargs)
            assert response.status_code == 200, (role, path, response.status_code)
            data = response.data
            if path != '/api/auth/me/':
                assert isinstance(data, (dict, list)), (path, type(data))
                if isinstance(data, dict):
                    assert 'results' in data and 'next' in data, path
            else:
                assert data['role'] == role and 'username' in data
            records = [data] if path == '/api/auth/me/' else (data['results'] if isinstance(data, dict) else data)
            for record in records:
                missing = required_fields[path.split('?')[0]] - set(record)
                assert not missing, (role, path, 'Missing native fields', missing)
            checked += 1
            print(f'PASS {role:6} GET {path}')
    print(f'{checked} authenticated read-only endpoint checks passed. No data changed.')


if __name__ == '__main__':
    main()
