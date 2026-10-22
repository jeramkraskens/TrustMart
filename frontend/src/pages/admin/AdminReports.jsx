import { useEffect, useState } from 'react';
import { FileDown, Printer, Users, Package, Receipt, TrendingUp } from 'lucide-react';
import client from '../../api/client';

const CARD = 'bg-[#151b26] border border-white/5 rounded-2xl';

function toCsv(rows, columns) {
  const header = columns.map((c) => c.label).join(',');
  const lines = rows.map((row) =>
    columns.map((c) => {
      const raw = c.value(row);
      const val = raw === null || raw === undefined ? '' : String(raw);
      return `"${val.replace(/"/g, '""')}"`;
    }).join(',')
  );
  return [header, ...lines].join('\n');
}

function downloadCsv(filename, csv) {
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

const REPORTS = [
  {
    key: 'users',
    icon: Users,
    title: 'Users report',
    description: 'Every account on the platform — role, trust score, ban status, join date.',
    color: 'text-teal',
    fetch: () => client.get('/auth/users/').then((r) => r.data.results ?? r.data),
    columns: [
      { label: 'ID', value: (u) => u.id },
      { label: 'Username', value: (u) => u.username },
      { label: 'Email', value: (u) => u.email },
      { label: 'Role', value: (u) => u.role },
      { label: 'Trust score', value: (u) => u.trust_score },
      { label: 'Banned', value: (u) => (u.is_banned ? 'Yes' : 'No') },
      { label: 'Joined', value: (u) => u.created_at?.slice(0, 10) },
    ],
  },
  {
    key: 'listings',
    icon: Package,
    title: 'Listings report',
    description: 'All listings across every status — draft, pending, verified, sold, rejected.',
    color: 'text-purple',
    fetch: () => client.get('/listings/', { params: { all: 'true' } }).then((r) => r.data.results ?? r.data),
    columns: [
      { label: 'ID', value: (l) => l.id },
      { label: 'Title', value: (l) => l.title },
      { label: 'Seller', value: (l) => l.seller_name },
      { label: 'Category', value: (l) => l.category_name },
      { label: 'Price', value: (l) => l.price },
      { label: 'Condition', value: (l) => l.condition },
      { label: 'Status', value: (l) => l.status },
      { label: 'Created', value: (l) => l.created_at?.slice(0, 10) },
    ],
  },
  {
    key: 'transactions',
    icon: Receipt,
    title: 'Transactions report',
    description: 'Escrow transactions — buyer, seller, amount, commission, status.',
    color: 'text-amber',
    fetch: () => client.get('/transactions/', { params: { all: 'true' } }).then((r) => r.data.results ?? r.data),
    columns: [
      { label: 'ID', value: (t) => t.id },
      { label: 'Listing', value: (t) => t.listing_title },
      { label: 'Buyer', value: (t) => t.buyer_name },
      { label: 'Seller', value: (t) => t.seller_name },
      { label: 'Amount', value: (t) => t.amount },
      { label: 'Escrow status', value: (t) => t.escrow_status },
      { label: 'Created', value: (t) => t.created_at?.slice(0, 10) },
    ],
  },
];

export default function AdminReports() {
  const [busyKey, setBusyKey] = useState('');
  const [summary, setSummary] = useState(null);

  useEffect(() => {
    Promise.all([
      client.get('/auth/admin-analytics/').then((r) => r.data),
      client.get('/auth/admin-revenue/').then((r) => r.data).catch(() => null),
    ]).then(([analytics, revenue]) => setSummary({ analytics, revenue }));
  }, []);

  const generate = async (report) => {
    setBusyKey(report.key);
    try {
      const rows = await report.fetch();
      const csv = toCsv(rows, report.columns);
      const stamp = new Date().toISOString().slice(0, 10);
      downloadCsv(`trustmart_${report.key}_${stamp}.csv`, csv);
    } finally {
      setBusyKey('');
    }
  };

  const printSummary = () => window.print();

  return (
    <div className="p-4 sm:p-6 space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-3 print:hidden">
        <div>
          <h1 className="text-xl font-bold text-white">Reports</h1>
          <p className="text-sm text-white/40">Export platform data or print a summary for stakeholders.</p>
        </div>
        <button
          onClick={printSummary}
          className="flex items-center gap-2 text-sm font-semibold px-4 py-2 rounded-lg border border-white/10 text-white/70 hover:text-white hover:border-white/30 transition-colors"
        >
          <Printer size={15} /> Print platform summary
        </button>
      </div>

      {/* Printable summary */}
      {summary && (
        <div id="report-summary" className={`${CARD} p-6 print:border-0 print:bg-white print:text-black`}>
          <div className="flex items-center gap-2 mb-4">
            <TrendingUp size={18} className="text-teal" />
            <h2 className="font-bold text-white print:text-black">Platform summary — {new Date().toLocaleDateString()}</h2>
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            {[
              ['Total users', summary.analytics.totals.users],
              ['Total listings', summary.analytics.totals.listings],
              ['Verified listings', summary.analytics.totals.verified_listings],
              ['Total GMV', `Rs. ${Number(summary.analytics.totals.gmv).toLocaleString()}`],
              ['Sold listings', summary.analytics.totals.sold_listings],
              ['Open disputes', summary.analytics.totals.open_disputes],
              ['Reviews', summary.analytics.totals.reviews],
              ['Banned users', summary.analytics.totals.banned_users],
            ].map(([label, value]) => (
              <div key={label}>
                <p className="text-xs text-white/40 print:text-black/50">{label}</p>
                <p className="text-lg font-bold text-white print:text-black">{value}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4 print:hidden">
        {REPORTS.map((report) => {
          const Icon = report.icon;
          return (
            <div key={report.key} className={`${CARD} p-5 flex flex-col`}>
              <Icon size={20} className={report.color} />
              <h3 className="font-semibold text-white mt-3">{report.title}</h3>
              <p className="text-xs text-white/40 mt-1 flex-1">{report.description}</p>
              <button
                onClick={() => generate(report)}
                disabled={busyKey === report.key}
                className="mt-4 flex items-center justify-center gap-2 text-sm font-semibold px-4 py-2 rounded-lg bg-teal text-white hover:bg-teal-dark transition-colors disabled:opacity-50"
              >
                <FileDown size={15} />
                {busyKey === report.key ? 'Generating...' : 'Download CSV'}
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
}
