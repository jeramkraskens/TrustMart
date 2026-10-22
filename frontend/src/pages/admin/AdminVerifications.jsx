import { useEffect, useState } from 'react';
import { getAll } from '../../api/marketplace';
import useRecordSearch from '../../components/useRecordSearch';
import { StatusBadge } from '../../components/Badges';

export default function AdminVerifications() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    getAll('/verifications/')
      .then(setItems)
      .catch(() => setError('Could not load verification requests. Please refresh.'))
      .finally(() => setLoading(false));
  }, []);

  const { filtered, searchControl } = useRecordSearch(statusFilter ? items.filter((v) => v.status === statusFilter) : items,
    ['listing_title', 'seller_name', 'moderator_name', 'status', 'id'], 'Search verification requests');

  return (
    <div className="p-6 md:p-8">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Verification requests</h1>
          <p className="text-white/40 text-sm">{filtered.length} request{filtered.length !== 1 ? 's' : ''}</p>
        </div>
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="px-4 py-2 rounded-lg border border-white/10 bg-[#1a212e] text-sm focus:outline-none focus:border-teal"
        >
          <option value="">All statuses</option>
          <option value="pending">Pending</option>
          <option value="approved">Approved</option>
          <option value="rejected">Rejected</option>
        </select>
      </div>

      {searchControl}
      {error && <p className="form-error" role="alert">{error}</p>}
      <div className="bg-[#1a212e] rounded-xl border border-white/5 overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-white/5 text-left text-white/40 text-xs uppercase tracking-wide">
              <th className="px-4 py-3 font-medium">Listing</th>
              <th className="px-4 py-3 font-medium">Seller</th>
              <th className="px-4 py-3 font-medium">Moderator</th>
              <th className="px-4 py-3 font-medium">Status</th>
              <th className="px-4 py-3 font-medium">Feedback</th>
              <th className="px-4 py-3 font-medium">Submitted</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={6} className="px-4 py-8 text-center text-white/30">Loading...</td></tr>
            ) : filtered.length === 0 ? (
              <tr><td colSpan={6} className="px-4 py-8 text-center text-white/30">No verification requests found.</td></tr>
            ) : (
              filtered.map((v) => (
                <tr key={v.id} className="border-b border-white/5 last:border-0 hover:bg-white/5">
                  <td className="px-4 py-3 font-medium">{v.listing_title}</td>
                  <td className="px-4 py-3 text-white/60">{v.seller_name}</td>
                  <td className="px-4 py-3 text-white/60">{v.moderator_name || '—'}</td>
                  <td className="px-4 py-3"><StatusBadge status={v.status} /></td>
                  <td className="px-4 py-3 text-xs text-white/40 max-w-xs truncate">{v.feedback || '—'}</td>
                  <td className="px-4 py-3 text-xs text-white/30">{v.created_at?.slice(0, 10)}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
