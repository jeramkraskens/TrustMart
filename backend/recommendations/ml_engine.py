"""
Content-based recommendation engine for TrustMart.

How it works:
1. Every verified listing's text (title + description + category) is vectorized
   using TF-IDF (Term Frequency - Inverse Document Frequency). This turns each
   listing into a numeric vector representing what it's "about".
2. Whenever a buyer searches or views a listing, that's logged as training data
   (SearchQuery / ListingView).
3. When recommendations are requested for a buyer, we build a "profile" out of
   their recent search queries and viewed listing titles, vectorize that profile
   the same way, and use cosine similarity to find the listings whose vectors
   are closest to the buyer's profile vector.
4. The trained vectorizer + listing matrix are cached to disk (joblib) so we
   don't have to refit on every request — retraining happens after a search,
   or on demand from the admin panel.
"""
import time
from pathlib import Path

import joblib
from django.conf import settings
from django.utils import timezone
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

CACHE_DIR = Path(settings.BASE_DIR) / 'ml_cache'
CACHE_FILE = CACHE_DIR / 'recommender.pkl'
RETRAIN_COOLDOWN_SECONDS = 5  # avoid retraining on every single keystroke/search burst


def _listing_text(listing):
    parts = [listing.title, listing.description, listing.condition]
    if listing.category_id:
        parts.append(listing.category.name)
    return ' '.join(p for p in parts if p)


def train_model(force=False):
    """(Re)fit the TF-IDF model over all verified listings and cache it to disk."""
    from listings.models import Listing

    if not force and CACHE_FILE.exists():
        age = time.time() - CACHE_FILE.stat().st_mtime
        if age < RETRAIN_COOLDOWN_SECONDS:
            return load_model(skip_train=True)

    listings = list(Listing.objects.filter(status='verified').select_related('category'))
    if not listings:
        return {'trained_at': None, 'num_listings': 0, 'vocabulary_size': 0}

    corpus = [_listing_text(l) for l in listings]
    listing_ids = [l.id for l in listings]

    vectorizer = TfidfVectorizer(stop_words='english', max_features=2000)
    matrix = vectorizer.fit_transform(corpus)

    CACHE_DIR.mkdir(exist_ok=True)
    payload = {
        'vectorizer': vectorizer,
        'matrix': matrix,
        'listing_ids': listing_ids,
        'trained_at': timezone.now().isoformat(),
    }
    joblib.dump(payload, CACHE_FILE)

    return {
        'trained_at': payload['trained_at'],
        'num_listings': len(listing_ids),
        'vocabulary_size': len(vectorizer.vocabulary_),
    }


def load_model(skip_train=False):
    if not CACHE_FILE.exists():
        if skip_train:
            return None
        train_model(force=True)
    try:
        return joblib.load(CACHE_FILE)
    except Exception:
        if skip_train:
            return None
        train_model(force=True)
        return joblib.load(CACHE_FILE)


def get_model_status():
    from .models import SearchQuery
    from collections import Counter

    model = load_model(skip_train=True)
    total_searches = SearchQuery.objects.count()
    top_terms = Counter(
        SearchQuery.objects.values_list('query_text', flat=True)
    ).most_common(10)

    if model is None:
        return {
            'trained_at': None,
            'num_listings_indexed': 0,
            'vocabulary_size': 0,
            'total_search_queries': total_searches,
            'top_searched_terms': [{'term': t, 'count': c} for t, c in top_terms],
        }
    return {
        'trained_at': model['trained_at'],
        'num_listings_indexed': len(model['listing_ids']),
        'vocabulary_size': len(model['vectorizer'].vocabulary_),
        'total_search_queries': total_searches,
        'top_searched_terms': [{'term': t, 'count': c} for t, c in top_terms],
    }


def _build_profile_text(user):
    from .models import SearchQuery, ListingView

    recent_searches = SearchQuery.objects.filter(user=user).order_by('-created_at')[:20]
    recent_views = ListingView.objects.filter(user=user).select_related('listing').order_by('-created_at')[:20]

    parts = [s.query_text for s in recent_searches]
    parts += [v.listing.title for v in recent_views]
    return ' '.join(parts)


def recommend_for_user(user, top_n=8):
    """Return up to top_n recommended Listing objects for this buyer."""
    from listings.models import Listing

    model = load_model()
    if model is None or not model['listing_ids']:
        return _fallback_recommendations(user, top_n)

    profile_text = _build_profile_text(user)
    if not profile_text.strip():
        return _fallback_recommendations(user, top_n)

    profile_vector = model['vectorizer'].transform([profile_text])
    similarities = cosine_similarity(profile_vector, model['matrix']).flatten()

    ranked_ids = [model['listing_ids'][i] for i in similarities.argsort()[::-1]]
    ranked_scores = sorted(similarities, reverse=True)

    # Exclude the buyer's own listings (if they also sell) and anything with ~0 relevance
    exclude_ids = set(Listing.objects.filter(seller=user).values_list('id', flat=True))

    results = []
    for listing_id, score in zip(ranked_ids, ranked_scores):
        if listing_id in exclude_ids or score <= 0:
            continue
        results.append(listing_id)
        if len(results) >= top_n:
            break

    if not results:
        return _fallback_recommendations(user, top_n)

    listings_by_id = Listing.objects.filter(id__in=results).select_related('category', 'seller')
    order = {lid: i for i, lid in enumerate(results)}
    return sorted(listings_by_id, key=lambda l: order[l.id])


def _fallback_recommendations(user, top_n):
    """No search/view history yet — show trending (recently verified) listings instead."""
    from listings.models import Listing
    return list(
        Listing.objects.filter(status='verified')
        .exclude(seller=user)
        .select_related('category', 'seller')
        .order_by('-created_at')[:top_n]
    )
