from unittest.mock import patch

from django.contrib.auth import get_user_model
from django.test import TestCase
from rest_framework.test import APIClient

from listings.models import Listing
from notifications.models import Notification
from transactions.models import Transaction
from .models import Dispute


class ModeratorResolutionTests(TestCase):
    def setUp(self):
        users = get_user_model().objects
        self.moderator = users.create_user(username='moderator', role='moderator')
        self.buyer = users.create_user(username='buyer', role='buyer')
        self.seller = users.create_user(username='seller', role='seller')
        listing = Listing.objects.create(seller=self.seller, title='Phone', price='100.00')
        self.txn = Transaction.objects.create(buyer=self.buyer, listing=listing, amount='100.00')
        self.dispute = Dispute.objects.create(transaction=self.txn, raised_by=self.buyer, reason='Wrong item')

    def resolve(self, resolution='buyer_favor', user=None, dispute=None):
        client = APIClient()
        client.force_authenticate(user=user or self.moderator)
        return client.post(f'/api/disputes/{(dispute or self.dispute).pk}/resolve/', {
            'resolution': resolution, 'resolution_notes': 'Reviewed evidence',
        }, format='json')

    def test_both_decisions_settle_escrow_and_record_moderator(self):
        for resolution, escrow, recipient in [
            ('buyer_favor', 'refunded', self.buyer),
            ('seller_favor', 'released', self.seller),
        ]:
            with self.subTest(resolution=resolution):
                self.txn.escrow_status = 'held'
                self.txn.save()
                self.dispute.status = 'open'
                self.dispute.save()
                self.assertEqual(self.resolve(resolution).status_code, 200)
                self.dispute.refresh_from_db()
                self.txn.refresh_from_db()
                self.assertEqual(self.txn.escrow_status, escrow)
                self.assertEqual(self.dispute.status, 'resolved')
                self.assertEqual(self.dispute.moderator, self.moderator)
                self.assertIsNotNone(self.dispute.resolved_at)
                self.assertTrue(Notification.objects.filter(user=recipient, type='dispute_resolved').exists())

    def test_repeated_decision_cannot_reverse_settlement_or_notify_twice(self):
        self.assertEqual(self.resolve().status_code, 200)
        for resolution in ['buyer_favor', 'seller_favor']:
            self.assertEqual(self.resolve(resolution).status_code, 400)
        self.txn.refresh_from_db()
        self.dispute.refresh_from_db()
        self.assertEqual(self.txn.escrow_status, 'refunded')
        self.assertEqual(self.dispute.resolution, 'buyer_favor')
        self.assertEqual(Notification.objects.filter(type='dispute_resolved').count(), 2)
        self.assertEqual(Notification.objects.filter(type='dispute_reviewed').count(), 1)

    def test_another_dispute_cannot_reverse_same_transaction(self):
        self.resolve()
        other = Dispute.objects.create(transaction=self.txn, raised_by=self.seller, reason='Other claim')
        self.assertEqual(self.resolve('seller_favor', dispute=other).status_code, 400)
        other.refresh_from_db()
        self.assertEqual(other.status, 'open')

    def test_failure_rolls_back_case_escrow_and_notification(self):
        with patch.object(Transaction, 'save', side_effect=RuntimeError('Database failure')):
            with self.assertRaises(RuntimeError):
                self.resolve()
        self.dispute.refresh_from_db()
        self.txn.refresh_from_db()
        self.assertEqual(self.dispute.status, 'open')
        self.assertIsNone(self.dispute.moderator)
        self.assertEqual(self.txn.escrow_status, 'held')
        self.assertEqual(Notification.objects.count(), 0)

    def test_invalid_resolution_and_non_moderator_are_rejected(self):
        self.assertEqual(self.resolve('invalid').status_code, 400)
        self.assertEqual(self.resolve(user=self.buyer).status_code, 403)
        self.dispute.refresh_from_db()
        self.assertEqual(self.dispute.status, 'open')
