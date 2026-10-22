# HTTP request handlers for recorded searches, listing views and product suggestions.
# Read the permission settings, then the query filters, then the action methods to follow a request.
from rest_framework import permissions, status
from rest_framework.response import Response
from rest_framework.views import APIView

from accounts.permissions import IsAdmin
from listings.serializers import ListingSerializer
from .models import SearchQuery, ListingView
from . import ml_engine


class LogSearchView(APIView):
    """Buyer performs a search -> log it as training data, then retrain the model
    so the next recommendation fetch reflects this search."""
    permission_classes = [permissions.IsAuthenticated]

    def post(self, request):
        query_text = (request.data.get('query') or '').strip()
        if not query_text:
            return Response({'detail': 'query is required.'}, status=status.HTTP_400_BAD_REQUEST)

        SearchQuery.objects.create(user=request.user, query_text=query_text)
        stats = ml_engine.train_model()

        return Response({
            'status': 'logged and retrained',
            'model_stats': stats,
        })


class LogListingViewView(APIView):
    """Buyer opens a listing's detail page -> log it as additional training signal."""
    permission_classes = [permissions.IsAuthenticated]

    def post(self, request):
        listing_id = request.data.get('listing')
        if not listing_id:
            return Response({'detail': 'listing is required.'}, status=status.HTTP_400_BAD_REQUEST)
        ListingView.objects.create(user=request.user, listing_id=listing_id)
        return Response({'status': 'logged'})


class RecommendationsView(APIView):
    """Returns recommended listings for the current buyer, based on their
    search and browsing history."""
    permission_classes = [permissions.IsAuthenticated]

    def get(self, request):
        listings = ml_engine.recommend_for_user(request.user, top_n=8)
        serializer = ListingSerializer(listings, many=True, context={'request': request})
        return Response(serializer.data)


class RecommenderStatusView(APIView):
    """Admin-only: see how the recommendation model is doing."""
    permission_classes = [IsAdmin]

    def get(self, request):
        return Response(ml_engine.get_model_status())


class RetrainRecommenderView(APIView):
    """Admin-only: manually trigger a full retrain of the recommendation model."""
    permission_classes = [IsAdmin]

    def post(self, request):
        stats = ml_engine.train_model(force=True)
        return Response({'status': 'retrained', 'model_stats': stats})
