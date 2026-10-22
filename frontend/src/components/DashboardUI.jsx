import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from "recharts";
export function StatCard({ icon: Icon, label, value, detail, tone = "teal" }) {
  return (
    <div className="stat-card">
      <span className={`feature-icon tone-${tone}`}>
        <Icon size={20} />
      </span>
      <div>
        <strong>{value}</strong>
        <p>{label}</p>
        {detail && <small>{detail}</small>}
      </div>
    </div>
  );
}
export function DashboardChart({
  data,
  dataKey,
  color = "#169b8e",
  pale = "#dff5f3",
  money = false,
}) {
  return (
    <ResponsiveContainer width="100%" height={220}>
      <BarChart
        data={data}
        margin={{ top: 15, right: 8, left: -18, bottom: 0 }}
      >
        <CartesianGrid stroke="#eeeef1" vertical={false} />
        <XAxis
          dataKey="label"
          axisLine={false}
          tickLine={false}
          tick={{ fill: "#85858e", fontSize: 11 }}
          dy={8}
        />
        <YAxis
          axisLine={false}
          tickLine={false}
          tick={{ fill: "#aaaab3", fontSize: 10 }}
          tickFormatter={(v) =>
            v >= 1000000 ? `${v / 1000000}M` : v >= 1000 ? `${v / 1000}K` : v
          }
          allowDecimals={false}
        />
        <Tooltip
          cursor={{ fill: "#f8f8f8" }}
          contentStyle={{
            border: "1px solid #eee",
            borderRadius: 10,
            fontSize: 12,
          }}
          formatter={(v) => (money ? `Rs. ${Number(v).toLocaleString()}` : v)}
        />
        <Bar dataKey={dataKey} radius={[5, 5, 0, 0]} maxBarSize={55}>
          {data.map((row, i) => (
            <Cell key={row.month} fill={i === data.length - 1 ? color : pale} />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
export function StatePill({ value }) {
  const tone = ["verified", "released", "resolved", "active"].includes(value)
    ? "green"
    : ["held", "pending", "pending_verification", "draft"].includes(value)
      ? "orange"
      : ["rejected", "open", "refunded", "flagged"].includes(value)
        ? "red"
        : "purple";
  return (
    <span className={`tm-pill tone-${tone}`}>
      {{
        verified: "Active",
        pending_verification: "Pending",
        held: "Escrow",
        released: "Released",
      }[value] || value}
    </span>
  );
}
