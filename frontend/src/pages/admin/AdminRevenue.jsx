import { useEffect, useState } from 'react';
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import client from '../../api/client';

function StatCard({ label, value, sublabel, accent }) {
  return (
    <div className="bg-[#1a212e] rounded-xl border border-white/5 p-4">
      <p className="text-2xl font-bold" style={{ color: accent }}>{value}</p>
      <p className="text-xs text-white/40 mt-0.5">{label}</p>
      {sublabel && <p className="text-[11px] text-white/25 mt-0.5">{sublabel}</p>}
    </div>
  );
}

const money = (n) => `Rs. ${Number(n).toLocaleString(undefined, { maximumFractionDigits: 0 })}`;

export default function AdminRevenue() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    client.get('/auth/admin-revenue/')
      .then((res) => setData(res.data))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="p-8 text-white/40">Loading...</div>;
  if (!data) return <div className="p-8 text-rust">Could not load revenue analytics.</div>;

  const t = data.totals;

  return (
    <div className="p-6 md:p-8 space-y-8">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Revenue &amp; Commission</h1>
        <p className="text-white/40 text-sm">Platform earnings from completed sales (5% commission per sale)</p>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <StatCard label="Total commission earned" value={money(t.total_commission_earned)} sublabel="From released escrow only" accent="#0D9488" />
        <StatCard label="GMV (completed sales)" value={money(t.total_gmv_released)} sublabel={`${t.completed_sales_count} sales`} accent="#7C3AED" />
        <StatCard label="Pending commission" value={money(t.pending_commission)} sublabel="Still held in escrow" accent="#D97706" />
        <StatCard label="Escrow currently held" value={money(t.escrow_held_amount)} accent="#D97706" />
        <StatCard label="Refunded to buyers" value={money(t.refunded_amount)} accent="#DC2626" />
        <StatCard label="Average order value" value={money(t.average_order_value)} accent="#059669" />
      </div>

      <div className="grid md:grid-cols-2 gap-5">
        <div className="bg-[#1a212e] rounded-xl border border-white/5 p-5">
          <h2 className="font-bold text-sm mb-4">Commission earned by month (Rs.)</h2>
          {data.commission_by_month.length === 0 ? (
            <p className="text-sm text-white/30 py-10 text-center">No completed sales yet.</p>
          ) : (
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={data.commission_by_month}>
                <CartesianGrid strokeDasharray="3 3" stroke="#eee" />
                <XAxis dataKey="month" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip formatter={(v) => money(v)} />
                <Bar dataKey="commission" fill="#0D9488" radius={[4, 4, 0, 0]} name="Commission" />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className="bg-[#1a212e] rounded-xl border border-white/5 p-5">
          <h2 className="font-bold text-sm mb-4">GMV by month (Rs.)</h2>
          {data.commission_by_month.length === 0 ? (
            <p className="text-sm text-white/30 py-10 text-center">No completed sales yet.</p>
          ) : (
            <ResponsiveContainer width="100%" height={220}>
              <LineChart data={data.commission_by_month}>
                <CartesianGrid strokeDasharray="3 3" stroke="#eee" />
                <XAxis dataKey="month" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip formatter={(v) => money(v)} />
                <Line type="monotone" dataKey="gmv" stroke="#7C3AED" strokeWidth={2} dot={{ r: 3 }} name="GMV" />
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>

      <div className="grid md:grid-cols-2 gap-5">
        <div className="bg-[#1a212e] rounded-xl border border-white/5 p-5">
          <h2 className="font-bold text-sm mb-4">Top sellers by revenue</h2>
          {data.top_sellers.length === 0 ? (
            <p className="text-sm text-white/30">No completed sales yet.</p>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-white/30 text-xs uppercase border-b border-white/5">
                  <th className="py-2 font-medium">Seller</th>
                  <th className="py-2 font-medium text-right">Sales</th>
                  <th className="py-2 font-medium text-right">Revenue</th>
                  <th className="py-2 font-medium text-right">Commission paid</th>
                </tr>
              </thead>
              <tbody>
                {data.top_sellers.map((s) => (
                  <tr key={s.seller_id} className="border-b border-white/5 last:border-0">
                    <td className="py-2 font-medium">{s.seller_name}</td>
                    <td className="py-2 text-right text-white/55">{s.sales_count}</td>
                    <td className="py-2 text-right text-teal-dark font-medium">{money(s.revenue)}</td>
                    <td className="py-2 text-right text-white/40">{money(s.commission_paid)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="bg-[#1a212e] rounded-xl border border-white/5 p-5">
          <h2 className="font-bold text-sm mb-4">Revenue by category</h2>
          {data.revenue_by_category.length === 0 ? (
            <p className="text-sm text-white/30">No completed sales yet.</p>
          ) : (
            <div className="space-y-2">
              {data.revenue_by_category.map((c) => {
                const max = Math.max(...data.revenue_by_category.map((r) => r.revenue));
                return (
                  <div key={c.category} className="flex items-center gap-3">
                    <span className="text-sm text-white/60 w-28 truncate">{c.category}</span>
                    <div className="flex-1 h-2 bg-white/5 rounded-full overflow-hidden">
                      <div className="h-full bg-purple rounded-full" style={{ width: `${(c.revenue / max) * 100}%` }} />
                    </div>
                    <span className="text-xs text-white/40 w-24 text-right">{money(c.revenue)}</span>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
