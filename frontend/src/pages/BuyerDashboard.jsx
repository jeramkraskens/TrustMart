// Buyer workspace for viewing purchase activity and related marketplace information.
import { useEffect, useState } from 'react';
import client from '../api/client';
import { getAll } from '../api/marketplace';
import useRecordSearch from '../components/useRecordSearch';
import { StatusBadge } from '../components/Badges';

// Section: Buyer dashboard — purchase history, delivery, disputes and reviews.
export default function BuyerDashboard() {
  const [transactions, setTransactions] = useState([]);
  const { filtered, searchControl } = useRecordSearch(transactions, ['listing_title', 'seller_name', 'escrow_status', 'id'], 'Search my purchases');
  const [loadError, setLoadError] = useState('');
  const [loading, setLoading] = useState(true);
  const [activeDispute, setActiveDispute] = useState(null);
  const [disputeReason, setDisputeReason] = useState('');
  const [activeReview, setActiveReview] = useState(null);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState('');
  const [notice, setNotice] = useState('');

  const load = () => getAll('/transactions/').then(setTransactions).catch(() => setLoadError('Could not load purchases. Please refresh.')).finally(() => setLoading(false));

  useEffect(() => { load(); }, []);

  // Section: Delivery confirmation — release demo escrow after buyer confirmation.
  const confirmDelivery = async (txn) => {
    await client.post(`/transactions/${txn.id}/confirm_delivery/`);
    setNotice('Delivery confirmed. Payment released to seller.');
    load();
  };

  const submitDispute = async (e) => {
    e.preventDefault();
    await client.post('/disputes/', { transaction: activeDispute.id, reason: disputeReason });
    setActiveDispute(null);
    setDisputeReason('');
    setNotice('Dispute submitted. A moderator will review it.');
    load();
  };

  const submitReview = async (e) => {
    e.preventDefault();
    await client.post('/reviews/', {
      transaction: activeReview.id,
      reviewee: activeReview.seller_id,
      rating: reviewRating,
      comment: reviewComment,
    });
    setActiveReview(null);
    setReviewComment('');
    setNotice('Thanks for your review!');
    load();
  };

  return (
    <div className="max-w-4xl mx-auto px-6 py-10">
      <h1 className="font-bold tracking-tight text-3xl mb-8">My purchases</h1>
      {searchControl}
      {loadError && <p className="form-error" role="alert">{loadError}</p>}
      {notice && <p className="text-sm text-sage mb-4 bg-sage-light rounded-lg px-4 py-2">{notice}</p>}

      {loading ? (
        <p className="text-ink/50">Loading...</p>
      ) : transactions.length === 0 ? (
        <div className="text-center py-20 text-ink/50">
          <p className="font-bold tracking-tight text-xl mb-2">No purchases yet</p>
          <p>Browse verified listings to make your first purchase.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {filtered.map((t) => (
            <div key={t.id} className="p-5 rounded-xl border border-ink/10 bg-white">
              <div className="flex items-center justify-between mb-3">
                <div>
                  <p className="font-medium">{t.listing_title}</p>
                  <p className="text-sm text-ink/50">Sold by {t.seller_name} · Rs. {Number(t.amount).toLocaleString()}</p>
                </div>
                <StatusBadge status={t.escrow_status} />
              </div>

              <div className="flex flex-wrap gap-2">
                {t.escrow_status === 'held' && !t.delivery_confirmed && (
                  <>
                    <button onClick={() => confirmDelivery(t)}
                      className="text-sm px-4 py-1.5 rounded-full bg-teal text-white hover:bg-teal-dark">
                      Confirm delivery
                    </button>
                    <button onClick={() => setActiveDispute(t)}
                      className="text-sm px-4 py-1.5 rounded-full border border-rust/40 text-rust hover:bg-rust-light">
                      Report a problem
                    </button>
                  </>
                )}
                {t.escrow_status === 'released' && (
                  <button onClick={() => setActiveReview(t)}
                    className="text-sm px-4 py-1.5 rounded-full border border-brass/40 text-brass hover:bg-brass-light">
                    Leave a review
                  </button>
                )}
              </div>

              {activeDispute?.id === t.id && (
                <form onSubmit={submitDispute} className="mt-4 pt-4 border-t border-ink/10 space-y-2">
                  <textarea value={disputeReason} onChange={(e) => setDisputeReason(e.target.value)} required rows={3}
                    placeholder="Describe the issue with this order..."
                    className="w-full px-3 py-2 rounded-lg border border-ink/15 bg-white text-sm focus:outline-none focus:border-rust" />
                  <div className="flex gap-2">
                    <button type="submit" className="text-sm px-4 py-1.5 rounded-full bg-rust text-white">Submit dispute</button>
                    <button type="button" onClick={() => setActiveDispute(null)} className="text-sm px-4 py-1.5 text-ink/50">Cancel</button>
                  </div>
                </form>
              )}

              {activeReview?.id === t.id && (
                <form onSubmit={submitReview} className="mt-4 pt-4 border-t border-ink/10 space-y-2">
                  <div className="flex gap-1">
                    {[1, 2, 3, 4, 5].map((n) => (
                      <button type="button" key={n} onClick={() => setReviewRating(n)}
                        className={`text-xl ${n <= reviewRating ? 'text-brass' : 'text-ink/20'}`}>★</button>
                    ))}
                  </div>
                  <textarea value={reviewComment} onChange={(e) => setReviewComment(e.target.value)} rows={2}
                    placeholder="How was the item and the seller?"
                    className="w-full px-3 py-2 rounded-lg border border-ink/15 bg-white text-sm focus:outline-none focus:border-brass" />
                  <div className="flex gap-2">
                    <button type="submit" className="text-sm px-4 py-1.5 rounded-full bg-teal text-white">Submit review</button>
                    <button type="button" onClick={() => setActiveReview(null)} className="text-sm px-4 py-1.5 text-ink/50">Cancel</button>
                  </div>
                </form>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
