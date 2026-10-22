import { useEffect, useState } from 'react';
import { getAll } from '../../api/marketplace';
import useRecordSearch from '../../components/useRecordSearch';
import { StatusBadge } from '../../components/Badges';

export default function AdminTransactions() {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    getAll('/transactions/')
      .then(setTransactions)
      .catch(() => setError('Could not load transactions. Please refresh.'))
      .finally(() => setLoading(false));
  }, []);

  const { filtered, searchControl } = useRecordSearch(statusFilter ? transactions.filter((t) => t.escrow_status === statusFilter) : transactions,
    ['listing_title', 'buyer_name', 'seller_name', 'escrow_status', 'id'], 'Search orders and escrow');

  return (
    <div className="p-6 md:p-8">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Transactions</h1>
          <p className="text-white/40 text-sm">{filtered.length} transaction{filtered.length !== 1 ? 's' : ''}</p>
        </div>
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="px-4 py-2 rounded-lg border border-white/10 bg-[#1a212e] text-sm focus:outline-none focus:border-teal"
        >
          <option value="">All statuses</option>
          <option value="held">Held in escrow</option>
          <option value="released">Released</option>
          <option value="refunded">Refunded</option>
        </select>
      </div>

      {searchControl}
      {error && <p className="form-error" role="alert">{error}</p>}
      <div className="bg-[#1a212e] rounded-xl border border-white/5 overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-white/5 text-left text-white/40 text-xs uppercase tracking-wide">
              <th className="px-4 py-3 font-medium">Listing</th>
              <th className="px-4 py-3 font-medium">Buyer</th>
              <th className="px-4 py-3 font-medium">Seller</th>
              <th className="px-4 py-3 font-medium">Amount</th>
              <th className="px-4 py-3 font-medium">Escrow status</th>
              <th className="px-4 py-3 font-medium">Date</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={6} className="px-4 py-8 text-center text-white/30">Loading...</td></tr>
            ) : filtered.length === 0 ? (
              <tr><td colSpan={6} className="px-4 py-8 text-center text-white/30">No transactions found.</td></tr>
            ) : (
              filtered.map((t) => (
                <tr key={t.id} className="border-b border-white/5 last:border-0 hover:bg-white/5">
                  <td className="px-4 py-3 font-medium">{t.listing_title}</td>
                  <td className="px-4 py-3 text-white/60">{t.buyer_name}</td>
                  <td className="px-4 py-3 text-white/60">{t.seller_name}</td>
                  <td className="px-4 py-3 text-teal-dark font-medium">Rs. {Number(t.amount).toLocaleString()}</td>
                  <td className="px-4 py-3"><StatusBadge status={t.escrow_status} /></td>
                  <td className="px-4 py-3 text-xs text-white/30">{t.created_at?.slice(0, 10)}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
