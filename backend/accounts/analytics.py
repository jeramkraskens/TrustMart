# Builds account and revenue summaries for the administrator dashboard.
from datetime import timedelta
from decimal import Decimal

from django.contrib.auth import get_user_model
from django.db.models import Count, Sum, Avg, F, DecimalField, ExpressionWrapper
from django.db.models.functions import TruncMonth
from django.utils import timezone
from rest_framework.response import Response
from rest_framework.views import APIView

from .permissions import IsAdmin
from listings.models import Listing, Category
from transactions.models import Transaction
from disputes.models import Dispute
from verification.models import VerificationRequest
from reviews.models import Review

User = get_user_model()

COMMISSION_EXPR = ExpressionWrapper(
    F('amount') * F('commission_rate'), output_field=DecimalField(max_digits=12, decimal_places=2)
)


class AdminAnalyticsView(APIView):
    permission_classes = [IsAdmin]

    def get(self, request):
        six_months_ago = timezone.now() - timedelta(days=180)

        totals = {
            'users': User.objects.count(),
            'sellers': User.objects.filter(role='seller').count(),
            'buyers': User.objects.filter(role='buyer').count(),
            'moderators': User.objects.filter(role='moderator').count(),
            'banned_users': User.objects.filter(is_banned=True).count(),
            'listings': Listing.objects.count(),
            'verified_listings': Listing.objects.filter(status='verified').count(),
            'pending_verifications': VerificationRequest.objects.filter(status='pending').count(),
            'sold_listings': Listing.objects.filter(status='sold').count(),
            'open_disputes': Dispute.objects.filter(status='open').count(),
            'resolved_disputes': Dispute.objects.filter(status='resolved').count(),
            'transactions': Transaction.objects.count(),
            'gmv': Transaction.objects.aggregate(s=Sum('amount'))['s'] or 0,
            'escrow_held': Transaction.objects.filter(escrow_status='held').aggregate(s=Sum('amount'))['s'] or 0,
            'reviews': Review.objects.count(),
            'avg_rating': round(Review.objects.aggregate(a=Avg('rating'))['a'] or 0, 2),
        }

        user_growth_qs = (
            User.objects.filter(created_at__gte=six_months_ago)
            .annotate(month=TruncMonth('created_at'))
            .values('month').annotate(count=Count('id')).order_by('month')
        )
        user_growth = [{'month': r['month'].strftime('%Y-%m'), 'count': r['count']} for r in user_growth_qs]

        gmv_qs = (
            Transaction.objects.filter(created_at__gte=six_months_ago)
            .annotate(month=TruncMonth('created_at'))
            .values('month').annotate(amount=Sum('amount')).order_by('month')
        )
        gmv_by_month = [{'month': r['month'].strftime('%Y-%m'), 'amount': float(r['amount'] or 0)} for r in gmv_qs]

        listings_by_category = list(
            Category.objects.annotate(count=Count('listings')).values('name', 'count').order_by('-count')
        )

        recent_users = list(
            User.objects.order_by('-created_at').values(
                'id', 'username', 'role', 'created_at', 'is_banned'
            )[:6]
        )
        for u in recent_users:
            u['created_at'] = u['created_at'].strftime('%Y-%m-%d')

        recent_disputes = list(
            Dispute.objects.filter(status='open').order_by('-created_at').values(
                'id', 'reason', 'created_at'
            )[:6]
        )
        for d in recent_disputes:
            d['created_at'] = d['created_at'].strftime('%Y-%m-%d')

        order_status_breakdown = [
            {'status': 'Held in Escrow', 'count': Transaction.objects.filter(escrow_status='held').count()},
            {'status': 'Released', 'count': Transaction.objects.filter(escrow_status='released').count()},
            {'status': 'Refunded', 'count': Transaction.objects.filter(escrow_status='refunded').count()},
        ]

        listing_status_breakdown = [
            {'status': 'Draft', 'count': Listing.objects.filter(status='draft').count()},
            {'status': 'Pending Verification', 'count': Listing.objects.filter(status='pending_verification').count()},
            {'status': 'Verified', 'count': Listing.objects.filter(status='verified').count()},
            {'status': 'Rejected', 'count': Listing.objects.filter(status='rejected').count()},
            {'status': 'Sold', 'count': Listing.objects.filter(status='sold').count()},
        ]

        return Response({
            'totals': totals,
            'user_growth': user_growth,
            'gmv_by_month': gmv_by_month,
            'listings_by_category': listings_by_category,
            'recent_users': recent_users,
            'recent_disputes': recent_disputes,
            'order_status_breakdown': order_status_breakdown,
            'listing_status_breakdown': listing_status_breakdown,
        })


class AdminRevenueView(APIView):
    """Admin-only: platform commission and revenue analytics."""
    permission_classes = [IsAdmin]

    def get(self, request):
        six_months_ago = timezone.now() - timedelta(days=180)

        released = Transaction.objects.filter(escrow_status='released')
        held = Transaction.objects.filter(escrow_status='held')
        refunded = Transaction.objects.filter(escrow_status='refunded')

        released_with_commission = released.annotate(commission=COMMISSION_EXPR)
        held_with_commission = held.annotate(commission=COMMISSION_EXPR)

        totals = {
            'total_commission_earned': released_with_commission.aggregate(s=Sum('commission'))['s'] or Decimal('0'),
            'total_gmv_released': released.aggregate(s=Sum('amount'))['s'] or Decimal('0'),
            'pending_commission': held_with_commission.aggregate(s=Sum('commission'))['s'] or Decimal('0'),
            'escrow_held_amount': held.aggregate(s=Sum('amount'))['s'] or Decimal('0'),
            'refunded_amount': refunded.aggregate(s=Sum('amount'))['s'] or Decimal('0'),
            'completed_sales_count': released.count(),
            'average_order_value': released.aggregate(a=Avg('amount'))['a'] or Decimal('0'),
        }

        commission_by_month_qs = (
            released_with_commission.filter(updated_at__gte=six_months_ago)
            .annotate(month=TruncMonth('updated_at'))
            .values('month')
            .annotate(commission=Sum('commission'), gmv=Sum('amount'))
            .order_by('month')
        )
        commission_by_month = [
            {
                'month': r['month'].strftime('%Y-%m'),
                'commission': float(r['commission'] or 0),
                'gmv': float(r['gmv'] or 0),
            }
            for r in commission_by_month_qs
        ]

        top_sellers_qs = (
            released_with_commission.values('listing__seller__id', 'listing__seller__username')
            .annotate(revenue=Sum('amount'), commission_paid=Sum('commission'), sales_count=Count('id'))
            .order_by('-revenue')[:10]
        )
        top_sellers = [
            {
                'seller_id': r['listing__seller__id'],
                'seller_name': r['listing__seller__username'],
                'revenue': float(r['revenue'] or 0),
                'commission_paid': float(r['commission_paid'] or 0),
                'sales_count': r['sales_count'],
            }
            for r in top_sellers_qs
        ]

        revenue_by_category_qs = (
            released.values('listing__category__name')
            .annotate(revenue=Sum('amount'), sales_count=Count('id'))
            .order_by('-revenue')
        )
        revenue_by_category = [
            {
                'category': r['listing__category__name'] or 'Uncategorized',
                'revenue': float(r['revenue'] or 0),
                'sales_count': r['sales_count'],
            }
            for r in revenue_by_category_qs
        ]

        return Response({
            'totals': totals,
            'commission_by_month': commission_by_month,
            'top_sellers': top_sellers,
            'revenue_by_category': revenue_by_category,
        })
