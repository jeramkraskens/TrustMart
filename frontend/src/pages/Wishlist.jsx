// Saved-listings screen. Saving a listing is separate from purchasing it.
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Heart } from 'lucide-react';
import { getAll } from '../api/marketplace';
import useRecordSearch from '../components/useRecordSearch';
import ListingCard from '../components/ListingCard';

export default function Wishlist() {
  const [items, setItems] = useState(null);
  const { filtered, searchControl } = useRecordSearch(items, ['listing_detail.title', 'listing_detail.category_name', 'listing_detail.location'], 'Search saved listings');
  const [error, setError] = useState('');

  const load = () => getAll('/wishlist/').then(setItems).catch(() => setError('Could not load saved listings. Please refresh.'));

  useEffect(() => { load(); }, []);

  const handleWishlistChange = (listingId, wishlisted) => {
    if (!wishlisted) {
      setItems((prev) => prev.filter((w) => w.listing !== listingId));
    }
  };

  return (
    <div className="max-w-6xl mx-auto px-6 py-10">
      <div className="flex items-center gap-2 mb-1">
        <Heart size={22} className="text-rust" />
        <h1 className="font-bold tracking-tight text-3xl">My wishlist</h1>
      </div>
      <p className="text-ink/50 text-sm mb-8">
        Items you've saved. Since every listing here is a unique, individually-verified item,
        saving to your wishlist is how you build a shortlist before buying — there's no shared cart to check out.
      </p>

      {searchControl}
      {error ? <p className="form-error" role="alert">{error}</p> : items === null ? (
        <p className="text-ink/50">Loading...</p>
      ) : items.length === 0 ? (
        <div className="text-center py-20 text-ink/50">
          <p className="font-bold tracking-tight text-xl mb-2">Your wishlist is empty</p>
          <p className="mb-4">Tap the heart icon on any listing to save it here.</p>
          <Link to="/" className="inline-block text-sm font-semibold px-5 py-2.5 rounded-lg bg-teal text-white hover:bg-teal-dark transition-colors">
            Browse listings
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {filtered.map((w) => (
            <ListingCard
              key={w.id}
              listing={w.listing_detail}
              wishlisted
              onWishlistChange={handleWishlistChange}
            />
          ))}
        </div>
      )}
    </div>
  );
}
