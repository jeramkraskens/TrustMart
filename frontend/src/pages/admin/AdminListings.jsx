import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import client, { mediaUrl } from '../../api/client';
import { StatusBadge } from '../../components/Badges';
import { getAll } from '../../api/marketplace';
import useRecordSearch from '../../components/useRecordSearch';

const STATUSES = ['draft', 'pending_verification', 'verified', 'rejected', 'sold'];

export default function AdminListings() {
  const [listings, setListings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('');
  const { filtered, searchControl } = useRecordSearch(listings, ['title', 'seller_name', 'category_name', 'location', 'status'], 'Search admin listings');
  const [error, setError] = useState('');

  const load = () => {
    setLoading(true);
    const params = { all: 'true' };
    if (statusFilter) params.status = statusFilter;
    getAll('/listings/', params)
      .then((data) => { setListings(data); setError(''); })
      .catch(() => setError('Could not load listings. Please refresh.'))
      .finally(() => setLoading(false));
  };

  useEffect(load, [statusFilter]);

  const deleteListing = (l) => {
    if (!confirm(`Delete listing "${l.title}"? This cannot be undone.`)) return;
    client.delete(`/listings/${l.id}/`).then(load);
  };

  return (
    <div className="p-6 md:p-8">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Listings</h1>
          <p className="text-white/40 text-sm">{listings.length} listing{listings.length !== 1 ? 's' : ''}</p>
        </div>
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="px-4 py-2 rounded-lg border border-white/10 bg-[#1a212e] text-sm focus:outline-none focus:border-teal"
        >
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
        </select>
      </div>

      {searchControl}
      {error && <p role="alert" className="form-error">{error}</p>}
      <div className="bg-[#1a212e] rounded-xl border border-white/5 overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-white/5 text-left text-white/40 text-xs uppercase tracking-wide">
              <th className="px-4 py-3 font-medium">Listing</th>
              <th className="px-4 py-3 font-medium">Seller</th>
              <th className="px-4 py-3 font-medium">Price</th>
              <th className="px-4 py-3 font-medium">Status</th>
              <th className="px-4 py-3 font-medium text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={5} className="px-4 py-8 text-center text-white/30">Loading...</td></tr>
            ) : filtered.length === 0 ? (
              <tr><td colSpan={5} className="px-4 py-8 text-center text-white/30">No listings found.</td></tr>
            ) : (
              filtered.map((l) => (
                <tr key={l.id} className="border-b border-white/5 last:border-0 hover:bg-white/5">
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-lg bg-white/5 overflow-hidden flex-shrink-0">
                        {l.images?.[0] && <img src={mediaUrl(l.images[0].image)} className="w-full h-full object-cover" />}
                      </div>
                      <span className="font-medium truncate max-w-xs">{l.title}</span>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-white/60">{l.seller_name}</td>
                  <td className="px-4 py-3 text-teal-dark font-medium">Rs. {Number(l.price).toLocaleString()}</td>
                  <td className="px-4 py-3"><StatusBadge status={l.status} /></td>
                  <td className="px-4 py-3 text-right space-x-2">
                    <Link to={`/listings/${l.id}`} className="text-xs text-teal hover:underline">View</Link>
                    <button
                      onClick={() => deleteListing(l)}
                      className="text-xs px-3 py-1 rounded-full border border-white/10 text-white/40 hover:border-rust hover:text-rust"
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
