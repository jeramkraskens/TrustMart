# URL connections for recorded searches, listing views and product suggestions.
# The project router in config/urls.py includes these endpoints under its API prefix.
from django.urls import path
from .views import (
    LogSearchView, LogListingViewView, RecommendationsView,
    RecommenderStatusView, RetrainRecommenderView,
)

urlpatterns = [
    path('recommendations/', RecommendationsView.as_view(), name='recommendations'),
    path('recommendations/log-search/', LogSearchView.as_view(), name='log-search'),
    path('recommendations/log-view/', LogListingViewView.as_view(), name='log-view'),
    path('recommendations/status/', RecommenderStatusView.as_view(), name='recommender-status'),
    path('recommendations/retrain/', RetrainRecommenderView.as_view(), name='recommender-retrain'),
]
