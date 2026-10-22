import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  Users,
  Package,
  Banknote,
  LockKeyhole,
  Scale,
  ShieldCheck,
} from "lucide-react";
import client from "../../api/client";
import { Avatar } from "../../components/MarketplaceUI";
import { money } from "../../api/presentation";
import { DashboardChart, StatCard, StatePill } from "../../components/DashboardUI";
import { recentMonths } from "../../api/presentation";

export default function AdminOverview() {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [userFilter, setUserFilter] = useState("all");
  useEffect(() => {
    client
      .get("/auth/admin-analytics/")
      .then(({ data }) => setData(data))
      .catch(() =>
        setError(
          "Unable to load platform analytics. Please refresh to try again.",
        ),
      );
  }, []);
  if (error)
    return (
      <p className="dashboard-content form-error" role="alert">
        {error}
      </p>
    );
  if (!data)
    return <div className="empty-state">Loading platform overview...</div>;
  const t = data.totals;
  const growth = recentMonths().map((month) => ({
    ...month,
    count:
      data.user_growth.find((row) => row.month === month.month)?.count || 0,
  }));
  const gmv = recentMonths().map((month) => ({
    ...month,
    amount:
      data.gmv_by_month.find((row) => row.month === month.month)?.amount || 0,
  }));
  const users = data.recent_users.filter(
    (u) =>
      userFilter === "all" ||
      (userFilter === "flagged" ? u.is_banned : u.role === userFilter),
  );
  return (
    <div className="dashboard-content">
      <div className="stat-grid admin-stats">
        <StatCard
          icon={Users}
          label="Total Users"
          value={t.users.toLocaleString()}
          detail={`${t.buyers} buyers · ${t.sellers} sellers`}
          tone="blue"
        />
        <StatCard
          icon={Package}
          label="Active Listings"
          value={t.verified_listings.toLocaleString()}
          detail={`${t.listings} listings in total`}
        />
        <StatCard
          icon={Banknote}
          label="GMV This Month"
          value={money(gmv[gmv.length - 1].amount)}
          detail="Gross merchandise value"
          tone="green"
        />
        <StatCard
          icon={LockKeyhole}
          label="Escrow Held"
          value={money(t.escrow_held)}
          detail={`${data.order_status_breakdown.find((s) => s.status === "Held in Escrow")?.count || 0} active holds`}
          tone="orange"
        />
        <StatCard
          icon={Scale}
          label="Open Disputes"
          value={t.open_disputes}
          detail={`${t.resolved_disputes} resolved`}
          tone="red"
        />
        <StatCard
          icon={ShieldCheck}
          label="Pending Verification"
          value={t.pending_verifications}
          detail="Awaiting moderator review"
          tone="purple"
        />
      </div>
      <div className="admin-charts">
        <section className="tm-panel">
          <div className="dashboard-section-title">
            <div>
              <h2>User Growth</h2>
              <p>New registrations per month</p>
            </div>
          </div>
          <DashboardChart
            data={growth}
            dataKey="count"
            color="#111832"
            pale="#e0e9ff"
          />
        </section>
        <section className="tm-panel">
          <div className="dashboard-section-title">
            <div>
              <h2>GMV Overview</h2>
              <p>Gross merchandise value (Rs.)</p>
            </div>
          </div>
          <DashboardChart data={gmv} dataKey="amount" money />
        </section>
        <section className="tm-panel">
          <div className="dashboard-section-title">
            <h2>Needs Attention</h2>
            <span className="tm-pill tone-red">
              {t.open_disputes + t.pending_verifications}
            </span>
          </div>
          <Link to="/admin/disputes" className="alert-item">
            <span className="alert-dot" />
            {t.open_disputes} open disputes
          </Link>
          <Link to="/admin/verifications" className="alert-item">
            <span className="alert-dot" style={{ background: "#f59e0b" }} />
            {t.pending_verifications} items awaiting verification
          </Link>
          <Link to="/admin/users" className="alert-item">
            <span className="alert-dot" style={{ background: "#8b4cff" }} />
            {t.banned_users} banned accounts
          </Link>
          <Link to="/admin/transactions" className="alert-item">
            <span className="alert-dot" style={{ background: "#169b8e" }} />
            {money(t.escrow_held)} in escrow
          </Link>
          <Link to="/admin/reports" className="alert-item">
            <span className="alert-dot" style={{ background: "#3f82ff" }} />
            Download platform reports
          </Link>
        </section>
      </div>
      <div className="admin-tables">
        <section className="tm-panel">
          <div className="dashboard-section-title">
            <h2>Recent Users</h2>
            <Link className="text-link" to="/admin/users">
              View all
            </Link>
          </div>
          <div className="dashboard-tabs">
            {[
              ["all", "All"],
              ["buyer", "Buyers"],
              ["seller", "Sellers"],
              ["flagged", "Flagged"],
            ].map(([value, label]) => (
              <button
                key={value}
                className={userFilter === value ? "active" : ""}
                onClick={() => setUserFilter(value)}
              >
                {label}
              </button>
            ))}
          </div>
          <div className="dashboard-table-wrap">
            <table className="dashboard-table">
              <thead>
                <tr>
                  <th>User</th>
                  <th>Role</th>
                  <th>Joined</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id}>
                    <td>
                      <div className="table-user">
                        <Avatar name={u.username} />
                        {u.username}
                      </div>
                    </td>
                    <td className="capitalize">{u.role}</td>
                    <td>
                      {new Date(u.created_at).toLocaleDateString("en-GB", {
                        day: "numeric",
                        month: "short",
                      })}
                    </td>
                    <td>
                      <StatePill value={u.is_banned ? "flagged" : "active"} />
                    </td>
                    <td>
                      <Link className="text-link" to="/admin/users">
                        View
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!users.length && (
              <p className="empty-state">No recent users in this group.</p>
            )}
          </div>
        </section>
        <section className="tm-panel">
          <div className="dashboard-section-title">
            <h2>
              Open Disputes{" "}
              <span className="tm-pill tone-red">{t.open_disputes}</span>
            </h2>
            <Link className="text-link" to="/admin/disputes">
              View all
            </Link>
          </div>
          <div className="dashboard-table-wrap">
            <table className="dashboard-table">
              <thead>
                <tr>
                  <th>Case ID</th>
                  <th>Reason</th>
                  <th>Opened</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {data.recent_disputes.map((d) => (
                  <tr key={d.id}>
                    <td>#D-{String(d.id).padStart(4, "0")}</td>
                    <td>{d.reason}</td>
                    <td>
                      {new Date(d.created_at).toLocaleDateString("en-GB", {
                        day: "numeric",
                        month: "short",
                      })}
                    </td>
                    <td>
                      <Link
                        className="tm-button navy small"
                        to="/admin/disputes"
                      >
                        Review
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!data.recent_disputes.length && (
              <p className="empty-state">
                No open disputes. You're all caught up.
              </p>
            )}
          </div>
        </section>
      </div>
      <div className="admin-tables">
        <section className="tm-panel">
          <div className="dashboard-section-title">
            <h2>Verification Queue</h2>
            <Link className="text-link" to="/admin/verifications">
              Review queue →
            </Link>
          </div>
          {data.listing_status_breakdown.map((row) => (
            <div className="summary-row" key={row.status}>
              <span>{row.status}</span>
              <strong>{row.count}</strong>
            </div>
          ))}
        </section>
        <section className="tm-panel">
          <div className="dashboard-section-title">
            <h2>Transaction Overview</h2>
            <Link className="text-link" to="/admin/transactions">
              View all →
            </Link>
          </div>
          {data.order_status_breakdown.map((row) => (
            <div className="summary-row" key={row.status}>
              <span>{row.status}</span>
              <strong>{row.count}</strong>
            </div>
          ))}
          <div className="summary-row">
            <span>All-time GMV</span>
            <strong>{money(t.gmv)}</strong>
          </div>
        </section>
      </div>
    </div>
  );
}
