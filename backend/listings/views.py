# HTTP request handlers for product listings, categories, photos and saved items.
# Read the permission settings, then the query filters, then the action methods to follow a request.
from django.db.models import Q
from rest_framework import viewsets, permissions, filters
from rest_framework.decorators import action
from rest_framework.response import Response
from rest_framework.exceptions import ValidationError
from django.db import transaction
from django_filters.rest_framework import DjangoFilterBackend
from .models import Category, Listing, Wishlist
from .serializers import CategorySerializer, ListingSerializer, WishlistSerializer
from accounts.permissions import IsOwnerOrReadOnly
from .locations import LOCATIONS
from notifications.utils import notify, notify_staff


class ReadOnlyOrAdmin(permissions.BasePermission):
    def has_permission(self, request, view):
        if request.method in permissions.SAFE_METHODS:
            return True
        return bool(request.user and request.user.is_authenticated and
                    (request.user.role == 'admin' or request.user.is_superuser))


class CategoryViewSet(viewsets.ModelViewSet):
    queryset = Category.objects.all()
    serializer_class = CategorySerializer
    permission_classes = [ReadOnlyOrAdmin]


class ListingViewSet(viewsets.ModelViewSet):
    serializer_class = ListingSerializer
    permission_classes = [permissions.IsAuthenticatedOrReadOnly, IsOwnerOrReadOnly]
    owner_field = 'seller'
    filter_backends = [DjangoFilterBackend, filters.SearchFilter, filters.OrderingFilter]
    filterset_fields = ['category', 'status', 'condition', 'district', 'city']
    search_fields = ['title', 'description']
    ordering_fields = ['price', 'created_at']

    # Section: Listing queries — apply ownership, staff access and status filters.
    def get_queryset(self):
        qs = Listing.objects.all()
        user = self.request.user
        mine = self.request.query_params.get('mine')
        if mine == 'true' and user.is_authenticated:
            return qs.filter(seller=user)

        wants_all = self.request.query_params.get('all')
        is_staff = user.is_authenticated and (user.role in ('admin', 'moderator') or user.is_superuser)
        if wants_all and is_staff:
            return qs

        if self.request.query_params.get('status') is None:
            if is_staff:
                # Staff can always open any listing (e.g. from the moderation/admin queue).
                return qs
            if user.is_authenticated:
                # Everyone sees verified listings; a seller can also always open
                # their own listing regardless of its status (draft/pending/rejected).
                qs = qs.filter(Q(status='verified') | Q(seller=user))
            else:
                qs = qs.filter(status='verified')
        return qs

    # DRF calls this hook after validation, allowing the server to set ownership during saving.
    @transaction.atomic
    def perform_create(self, serializer):
        listing = serializer.save(seller=self.request.user, status='draft')
        if listing.status == Listing.Status.DRAFT:
            notify(listing.seller, 'listing_created', f'Draft "{listing.title}" saved. Submit it for verification when ready.', link='/seller')

    @action(detail=False, methods=['get'], permission_classes=[permissions.AllowAny])
    def locations(self, request):
        return Response(LOCATIONS)

    @action(detail=False, methods=['get'], permission_classes=[permissions.AllowAny])
    def suggestions(self, request):
        query = ' '.join(request.query_params.get('q', '').split())[:100]
        # Public search never suggests private drafts, even for signed-in staff.
        listings = Listing.objects.filter(status=Listing.Status.VERIFIED).select_related('category')
        category = request.query_params.get('category', '')
        if category.isdigit():
            listings = listings.filter(category_id=category)
        for term in query.split():
            listings = listings.filter(Q(title__icontains=term) | Q(description__icontains=term))
        results = [{
            'id': f'listing-{item.pk}', 'label': item.title, 'value': item.title,
            'detail': ' · '.join(filter(None, [item.category.name if item.category else '', item.location])),
            'kind': 'Listing', 'url': f'/listings/{item.pk}',
        } for item in listings.order_by('-created_at')[:6]]
        categories = Category.objects.filter(name__icontains=query).order_by('name')[:3]
        results += [{
            'id': f'category-{item.pk}', 'label': item.name, 'value': item.name,
            'detail': 'Browse this category', 'kind': 'Category', 'url': f'/browse?category={item.pk}',
        } for item in categories]
        if len(query) >= 2:
            from urllib.parse import urlencode
            locations = []
            for district, cities in LOCATIONS.items():
                if query.casefold() in district.casefold():
                    locations.append({'id': f'district-{district}', 'label': district, 'value': district,
                                      'detail': 'All towns in this district', 'kind': 'District',
                                      'url': '/browse?' + urlencode({'district': district})})
                for city in cities:
                    if city != district and query.casefold() in city.casefold():
                        locations.append({'id': f'city-{district}-{city}', 'label': city, 'value': city,
                                          'detail': district, 'kind': 'Town',
                                          'url': '/browse?' + urlencode({'district': district, 'city': city})})
            results += locations[:3]
        return Response(results)

    # Section: Edit protection — preserve sold items and listings with orders.
    def require_manageable(self, listing):
        if listing.status == Listing.Status.SOLD or listing.transactions.exists():
            raise ValidationError({'detail': 'Listings with orders or a sold status cannot be edited or deleted. Order history must be kept.'})

    @transaction.atomic
    def perform_update(self, serializer):
        listing = Listing.objects.select_for_update().get(pk=serializer.instance.pk)
        self.require_manageable(listing)
        serializer.instance = listing
        updated = serializer.save()
        if self.request.user.pk != updated.seller_id:
            notify(updated.seller, 'listing_updated', f'Staff updated your listing "{updated.title}".', link='/seller')

    @transaction.atomic
    def perform_destroy(self, instance):
        listing = Listing.objects.select_for_update().get(pk=instance.pk)
        self.require_manageable(listing)
        seller, title = listing.seller, listing.title
        listing.delete()
        notify(seller, 'listing_removed', f'Listing "{title}" was removed.', link='/seller')
        notify_staff('listing_removed', f'{self.request.user.username} removed listing "{title}".',
                     roles=('admin',), exclude=self.request.user.pk, link='/admin/listings')


class WishlistViewSet(viewsets.ModelViewSet):
    """A buyer's saved listings. Acts as the marketplace's 'cart' equivalent —
    since each listing is a unique item bought individually via escrow rather
    than a multi-item checkout, saving-for-later is the right shape here."""
    serializer_class = WishlistSerializer
    permission_classes = [permissions.IsAuthenticated]
    http_method_names = ['get', 'post', 'delete', 'head', 'options']

    def get_queryset(self):
        return Wishlist.objects.filter(user=self.request.user).select_related('listing', 'listing__category', 'listing__seller')

    @action(detail=False, methods=['post'])
    def toggle(self, request):
        listing_id = request.data.get('listing')
        existing = Wishlist.objects.filter(user=request.user, listing_id=listing_id).first()
        if existing:
            existing.delete()
            return Response({'wishlisted': False})
        Wishlist.objects.create(user=request.user, listing_id=listing_id)
        return Response({'wishlisted': True})
