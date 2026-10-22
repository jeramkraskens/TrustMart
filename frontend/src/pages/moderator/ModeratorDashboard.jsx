// Website moderation workspace for listing verification and dispute review.
import { useEffect, useState } from 'react';
import { Link, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import client, { mediaUrl } from '../../api/client';
import { StatusBadge } from '../../components/Badges';
import { getAll } from '../../api/marketplace';
import useRecordSearch from '../../components/useRecordSearch';

function Tabs() {
  const location = useLocation();
  const navigate = useNavigate();
  const tabs = [
    { path: '/moderator', label: 'Verifications' },
    { path: '/moderator/disputes', label: 'Disputes' },
  ];
  return (
    <div className="flex gap-2 mb-8 border-b border-ink/10">
      {tabs.map((t) => (
        <button
          key={t.path}
          onClick={() => navigate(t.path)}
          className={`px-4 py-2.5 text-sm border-b-2 -mb-px transition-colors ${
            location.pathname === t.path ? 'border-brass text-brass' : 'border-transparent text-ink/50 hover:text-ink'
          }`}
        >
          {t.label}
        </button>
      ))}
    </div>
  );
}

// Section: Verification queue — review pending listings with moderator feedback.
function VerificationQueue() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [feedback, setFeedback] = useState({});
  const { filtered, searchControl } = useRecordSearch(items, ['listing_title', 'seller_name', 'id'], 'Search pending verifications');
  const [error, setError] = useState('');

  const load = () => getAll('/verifications/').then((all) => {
    setItems(all.filter((v) => v.status === 'pending'));
    setLoading(false);
  }).catch(() => { setError('Could not load the queue. Please refresh.'); setLoading(false); });

  useEffect(() => { load(); }, []);

  const act = async (id, action) => {
    await client.post(`/verifications/${id}/${action}/`, { feedback: feedback[id] || '' });
    load();
  };

  if (loading) return <p className="text-ink/50">Loading...</p>;
  if (error) return <p role="alert" className="form-error">{error}</p>;
  if (items.length === 0) return <p className="text-center py-16 text-ink/50">No pending verifications. Queue is clear.</p>;

  return (
    <div className="space-y-4">
      {searchControl}
      {filtered.map((v) => (
        <div key={v.id} className="p-5 rounded-xl border border-ink/10 bg-white">
          <div className="flex items-start justify-between gap-4 mb-3">
            <div>
              <p className="font-medium">{v.listing_title}</p>
              <p className="text-sm text-ink/50">Seller: {v.seller_name}</p>
            </div>
            <StatusBadge status={v.status} />
          </div>
          {v.notes && <p className="text-sm text-ink/70 mb-2 bg-paper-warm rounded-lg p-3">{v.notes}</p>}
          {v.evidence && (
            <a href={mediaUrl(v.evidence)} target="_blank" rel="noreferrer" className="text-sm text-brass hover:underline block mb-3">
              View evidence file
            </a>
          )}
          <Link to={`/listings/${v.listing}`} className="text-sm text-ink/60 hover:underline block mb-3">
            View full listing →
          </Link>
          {v.listing_detail?.seller && (
            <Link to={`/messages?with=${v.listing_detail.seller}`} className="text-sm text-brass hover:underline block mb-3">
              Chat with seller
            </Link>
          )}
          <textarea
            placeholder="Feedback for the seller (optional for approval, recommended for rejection)"
            value={feedback[v.id] || ''}
            onChange={(e) => setFeedback({ ...feedback, [v.id]: e.target.value })}
            rows={2}
            className="w-full px-3 py-2 rounded-lg border border-ink/15 bg-white text-sm focus:outline-none focus:border-brass mb-3"
          />
          <div className="flex gap-2">
            <button onClick={() => act(v.id, 'approve')} className="text-sm px-4 py-1.5 rounded-full bg-sage text-white hover:opacity-90">
              Approve
            </button>
            <button onClick={() => act(v.id, 'reject')} className="text-sm px-4 py-1.5 rounded-full bg-rust text-white hover:opacity-90">
              Reject
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}

function DisputeQueue() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [notes, setNotes] = useState({});
  const { filtered, searchControl } = useRecordSearch(items, ['listing_title', 'raised_by_name', 'id'], 'Search open disputes');
  const [error, setError] = useState('');

  const load = () => getAll('/disputes/').then((all) => {
    setItems(all.filter((d) => d.status === 'open'));
    setLoading(false);
  }).catch(() => { setError('Could not load disputes. Please refresh.'); setLoading(false); });

  useEffect(() => { load(); }, []);

  const resolve = async (id, resolution) => {
    await client.post(`/disputes/${id}/resolve/`, { resolution, resolution_notes: notes[id] || '' });
    load();
  };

  if (loading) return <p className="text-ink/50">Loading...</p>;
  if (error) return <p role="alert" className="form-error">{error}</p>;
  if (items.length === 0) return <p className="text-center py-16 text-ink/50">No open disputes.</p>;

  return (
    <div className="space-y-4">
      {searchControl}
      {filtered.map((d) => (
        <div key={d.id} className="p-5 rounded-xl border border-ink/10 bg-white">
          <div className="flex items-start justify-between gap-4 mb-3">
            <div>
              <p className="font-medium">{d.listing_title}</p>
              <p className="text-sm text-ink/50">Raised by {d.raised_by_name}</p>
            </div>
            <StatusBadge status={d.status} />
          </div>
          <p className="text-sm text-ink/70 mb-3 bg-paper-warm rounded-lg p-3">{d.reason}</p>
          <textarea
            placeholder="Resolution notes"
            value={notes[d.id] || ''}
            onChange={(e) => setNotes({ ...notes, [d.id]: e.target.value })}
            rows={2}
            className="w-full px-3 py-2 rounded-lg border border-ink/15 bg-white text-sm focus:outline-none focus:border-brass mb-3"
          />
          <div className="flex gap-2">
            <button onClick={() => resolve(d.id, 'buyer_favor')} className="text-sm px-4 py-1.5 rounded-full border border-ink/20 hover:border-brass hover:text-brass">
              Refund buyer
            </button>
            <button onClick={() => resolve(d.id, 'seller_favor')} className="text-sm px-4 py-1.5 rounded-full border border-ink/20 hover:border-brass hover:text-brass">
              Release to seller
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}

export default function ModeratorDashboard() {
  return (
    <div className="max-w-4xl mx-auto px-6 py-10">
      <h1 className="font-bold tracking-tight text-3xl mb-1">Moderation queue</h1>
      <p className="text-ink/60 mb-6">Review listings and resolve disputes.</p>
      <Tabs />
      <Routes>
        <Route index element={<VerificationQueue />} />
        <Route path="disputes" element={<DisputeQueue />} />
      </Routes>
    </div>
  );
}
