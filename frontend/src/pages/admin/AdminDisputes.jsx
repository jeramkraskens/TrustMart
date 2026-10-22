import { useEffect, useState } from 'react';
import client from '../../api/client';
import { getAll } from '../../api/marketplace';
import useRecordSearch from '../../components/useRecordSearch';
import { StatusBadge } from '../../components/Badges';

export default function AdminDisputes() {
  const [disputes, setDisputes] = useState([]);
  const { filtered, searchControl } = useRecordSearch(disputes, ['listing_title', 'raised_by_name', 'status', 'id'], 'Search disputes');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [expandedId, setExpandedId] = useState(null);
  const [notes, setNotes] = useState({});

  const load = () => {
    setLoading(true);
    getAll('/disputes/').then(setDisputes).catch(() => setError('Could not load disputes. Please refresh.')).finally(() => setLoading(false));
  };

  useEffect(load, []);

  const resolve = (d, resolution) => {
    client.post(`/disputes/${d.id}/resolve/`, { resolution, resolution_notes: notes[d.id] || '' }).then(load);
  };

  return (
    <div className="p-6 md:p-8">
      <h1 className="text-2xl font-bold tracking-tight mb-1">Disputes</h1>
      <p className="text-white/40 text-sm mb-6">{disputes.length} total, {disputes.filter((d) => d.status === 'open').length} open</p>

      {searchControl}
      {error && <p className="form-error" role="alert">{error}</p>}
      <div className="space-y-3">
        {loading ? (
          <p className="text-white/30">Loading...</p>
        ) : disputes.length === 0 ? (
          <p className="text-white/30">No disputes.</p>
        ) : (
          filtered.map((d) => (
            <div key={d.id} className="bg-[#1a212e] rounded-xl border border-white/5 p-4">
              <div
                className="flex items-center justify-between cursor-pointer"
                onClick={() => setExpandedId(expandedId === d.id ? null : d.id)}
              >
                <div>
                  <p className="font-medium text-sm">{d.listing_title}</p>
                  <p className="text-xs text-white/40">Raised by {d.raised_by_name}</p>
                </div>
                <StatusBadge status={d.status} />
              </div>

              {expandedId === d.id && (
                <div className="mt-4 pt-4 border-t border-white/5">
                  <p className="text-sm bg-white/5 rounded-lg p-3 mb-3">{d.reason}</p>
                  {d.status === 'open' ? (
                    <>
                      <textarea
                        value={notes[d.id] || ''}
                        onChange={(e) => setNotes({ ...notes, [d.id]: e.target.value })}
                        placeholder="Resolution notes"
                        rows={2}
                        className="w-full px-3 py-2 rounded-lg border border-white/10 text-sm mb-3 focus:outline-none focus:border-teal"
                      />
                      <div className="flex gap-2">
                        <button onClick={() => resolve(d, 'buyer_favor')} className="text-xs px-4 py-1.5 rounded-full border border-white/10 hover:border-teal hover:text-teal">
                          Refund buyer
                        </button>
                        <button onClick={() => resolve(d, 'seller_favor')} className="text-xs px-4 py-1.5 rounded-full border border-white/10 hover:border-teal hover:text-teal">
                          Release to seller
                        </button>
                      </div>
                    </>
                  ) : (
                    <p className="text-xs text-white/40">
                      Resolved: {d.resolution === 'buyer_favor' ? 'Refunded to buyer' : 'Released to seller'}
                      {d.resolution_notes && ` — ${d.resolution_notes}`}
                    </p>
                  )}
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
}
