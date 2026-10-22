import { useEffect, useState } from 'react';
import client from '../../api/client';
import { getAll } from '../../api/marketplace';
import useRecordSearch from '../../components/useRecordSearch';

export default function AdminReviews() {
  const [reviews, setReviews] = useState([]);
  const { filtered, searchControl } = useRecordSearch(reviews, ['reviewer_name', 'reviewee_name', 'comment', 'rating'], 'Search reviews');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = () => {
    setLoading(true);
    getAll('/reviews/').then(setReviews).catch(() => setError('Could not load reviews. Please refresh.')).finally(() => setLoading(false));
  };

  useEffect(load, []);

  const deleteReview = (r) => {
    if (!confirm('Delete this review? This cannot be undone.')) return;
    client.delete(`/reviews/${r.id}/`).then(load);
  };

  return (
    <div className="p-6 md:p-8">
      <h1 className="text-2xl font-bold tracking-tight mb-1">Reviews</h1>
      <p className="text-white/40 text-sm mb-6">{reviews.length} review{reviews.length !== 1 ? 's' : ''} · moderate inappropriate content</p>

      {searchControl}
      {error && <p className="form-error" role="alert">{error}</p>}
      <div className="bg-[#1a212e] rounded-xl border border-white/5 divide-y divide-white/5">
        {loading ? (
          <p className="p-6 text-center text-white/30">Loading...</p>
        ) : reviews.length === 0 ? (
          <p className="p-6 text-center text-white/30">No reviews yet.</p>
        ) : (
          filtered.map((r) => (
            <div key={r.id} className="p-4 flex items-start justify-between gap-4">
              <div className="flex-1">
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-amber text-sm">{'★'.repeat(r.rating)}{'☆'.repeat(5 - r.rating)}</span>
                  <span className="text-xs text-white/30">{r.reviewer_name} → {r.reviewee_name}</span>
                </div>
                {r.comment && <p className="text-sm text-white/60">{r.comment}</p>}
                <p className="text-xs text-white/20 mt-1">{r.created_at?.slice(0, 10)}</p>
              </div>
              <button
                onClick={() => deleteReview(r)}
                className="text-xs px-3 py-1 rounded-full border border-white/10 text-white/40 hover:border-rust hover:text-rust flex-shrink-0"
              >
                Delete
              </button>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
