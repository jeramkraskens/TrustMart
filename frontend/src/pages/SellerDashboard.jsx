// Seller workspace for managing listings and viewing seller activity.
import { useEffect, useRef, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import {
  LayoutDashboard,
  Package,
  ShoppingBag,
  Wallet,
  MessageCircle,
  Scale,
  Star,
  Settings,
  LogOut,
  Plus,
  Banknote,
} from "lucide-react";
import { getAll } from "../api/marketplace";
import client, { mediaUrl } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { Avatar, Brand } from "../components/MarketplaceUI";
import { money, itemEmoji } from "../api/presentation";
import { StatCard, DashboardChart, StatePill } from "../components/DashboardUI";
import { recentMonths } from "../api/presentation";
import useRecordSearch from '../components/useRecordSearch';
import NotificationBell from '../components/NotificationBell';

// Section: Seller dashboard — manage listings, sales, reviews and disputes.
export default function SellerDashboard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [notice, setNotice] = useState(location.state?.notice || '');
  const [deleting, setDeleting] = useState(null);
  const [deleteError, setDeleteError] = useState('');
  const [busy, setBusy] = useState(false);
  const deleteDialog = useRef(null);
  useEffect(() => {
    if (deleting) deleteDialog.current?.showModal();
    else deleteDialog.current?.close();
  }, [deleting]);
  // Section: Listing deletion — update the table only after the API succeeds.
  const deleteListing = async () => {
    if (busy || !deleting) return;
    setBusy(true);
    setDeleteError('');
    try {
      await client.delete(`/listings/${deleting.id}/`);
      setData((old) => ({ ...old, listings: old.listings.filter((item) => item.id !== deleting.id) }));
      setNotice('Listing deleted.');
      setDeleting(null);
    } catch (e) { setDeleteError(e.response?.data?.detail || 'Could not delete this listing. Please try again.'); }
    finally { setBusy(false); }
  };
  const [data, setData] = useState(null);
  const { filtered: searchedListings, searchControl: listingSearch } = useRecordSearch(data?.listings, ['title', 'category_name', 'location', 'status'], 'Search my listings');
  const { filtered: searchedOrders, searchControl: orderSearch } = useRecordSearch(data?.orders, ['listing_title', 'buyer_name', 'escrow_status', 'id'], 'Search my orders');
  const { filtered: searchedReviews, searchControl: reviewSearch } = useRecordSearch(data?.reviews, ['reviewer_name', 'comment', 'rating'], 'Search my reviews');
  const { filtered: searchedDisputes, searchControl: disputeSearch } = useRecordSearch(data?.disputes, ['listing_title', 'reason', 'status', 'id'], 'Search my disputes');
  const [error, setError] = useState("");
  const [section, setSection] = useState("dashboard");
  const [filter, setFilter] = useState("all");
  const [months, setMonths] = useState(6);
  useEffect(() => {
    Promise.all([
      getAll("/listings/", { mine: "true" }),
      getAll("/transactions/"),
      getAll("/reviews/", { reviewee: user.id }),
      getAll("/messages/"),
      getAll("/disputes/"),
    ])
      .then(([listings, orders, reviews, messages, disputes]) =>
        setData({
          listings,
          orders: orders.filter((o) => o.seller_id === user.id),
          reviews,
          messages,
          disputes: disputes.filter((d) => d.seller_id === user.id),
        }),
      )
      .catch(() =>
        setError("Could not load your dashboard. Please refresh to try again."),
      );
  }, [user.id]);
  const name =
    [user.first_name, user.last_name].filter(Boolean).join(" ") ||
    user.username;
  const orders = data?.orders || [];
  const released = orders.filter((o) => o.escrow_status === "released");
  const revenue = released.reduce((sum, o) => sum + Number(o.seller_payout), 0);
  const held = orders
    .filter((o) => o.escrow_status === "held")
    .reduce((sum, o) => sum + Number(o.seller_payout), 0);
  const average = data?.reviews.length
    ? (
        data.reviews.reduce((sum, r) => sum + r.rating, 0) / data.reviews.length
      ).toFixed(1)
    : "—";
  const monthly = recentMonths(months).map((month) => ({
    ...month,
    amount: released
      .filter((o) => o.updated_at.startsWith(month.month))
      .reduce((sum, o) => sum + Number(o.seller_payout), 0),
  }));
  const nav = [
    ["dashboard", "Dashboard", LayoutDashboard],
    ["listings", "My Listings", Package],
    ["orders", "Orders", ShoppingBag],
    ["escrow", "Escrow", Wallet],
    ["messages", "Messages", MessageCircle],
    ["disputes", "Disputes", Scale],
    ["reviews", "Reviews", Star],
  ];
  const renderListings = () => (
    <section className="tm-panel">
      <div className="dashboard-section-title">
        <h2>
          My Listings <small>{data.listings.length} total</small>
        </h2>
        <Link className="text-link" to="/seller/new">
          Add listing →
        </Link>
      </div>
      <div className="dashboard-tabs">
        {["all", "verified", "pending_verification", "sold"].map((status) => (
          <button
            key={status}
            className={filter === status ? "active" : ""}
            onClick={() => setFilter(status)}
          >
            {
              {
                all: "All",
                verified: "Active",
                pending_verification: "Pending",
                sold: "Sold",
              }[status]
            }
          </button>
        ))}
      </div>
      {listingSearch}
      <div className="dashboard-table-wrap">
        <table className="dashboard-table">
          <thead>
            <tr>
              <th>Item</th>
              <th>Price</th>
              <th>Status</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {searchedListings
              .filter((item) => filter === "all" || item.status === filter)
              .map((item) => (
                <tr key={item.id}>
                  <td>
                    <div className="table-product">
                      {item.images?.[0] ? (
                        <img src={mediaUrl(item.images[0].image)} alt="" />
                      ) : (
                        <span>{itemEmoji(item)}</span>
                      )}
                      {item.title}
                    </div>
                  </td>
                  <td>{money(item.price)}</td>
                  <td>
                    <StatePill value={item.status} />
                  </td>
                  <td>
                    <div className="seller-listing-actions">
                    <Link
                      className="tm-button secondary small"
                      to={`/listings/${item.id}`}
                    >
                      View
                    </Link>
                    {item.can_manage ? <>
                      <Link className="tm-button secondary small" to={`/seller/listings/${item.id}/edit`}>Edit</Link>
                      <button className="tm-button secondary small danger-button" onClick={() => { setDeleting(item); setDeleteError(''); }}>Delete</button>
                    </> : <small className="muted">Order history retained</small>}
                    </div>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
        {!searchedListings.filter(
          (item) => filter === "all" || item.status === filter,
        ).length && <p className="empty-state">No listings in this view.</p>}
      </div>
    </section>
  );
  const renderOrders = (full = false) => (
    <section className="tm-panel">
      <div className="dashboard-section-title">
        <h2>{full ? "Orders" : "Recent Orders"}</h2>
        <span className="tm-pill tone-orange">{orders.length} total</span>
      </div>
      {full && orderSearch}
      {(full ? searchedOrders : orders.slice(0, 4)).map((order) => (
        <Link
          className="order-row"
          key={order.id}
          to={`/listings/${order.listing}`}
        >
          <Avatar name={order.buyer_name} />
          <div>
            <strong>{order.buyer_name}</strong>
            <p>{order.listing_title}</p>
          </div>
          <span>{money(order.amount)}</span>
          <StatePill value={order.escrow_status} />
        </Link>
      ))}
      {!orders.length && (
        <p className="muted">
          Your orders will appear here after your first sale.
        </p>
      )}
    </section>
  );
  const renderReviews = () => (
    <section className="tm-panel">
      <div className="dashboard-section-title">
        <h2>Seller Trust Profile</h2>
      </div>
      <div className="trust-score">
        <strong>{average}</strong>
        <div>
          <span className="stars">
            {data.reviews.length ? "★★★★★" : "☆☆☆☆☆"}
          </span>
          <p>{data.reviews.length} reviews</p>
        </div>
      </div>
      <span className="tm-pill tone-teal">Trust score: {user.trust_score}</span>
      {section === "reviews" &&
        reviewSearch}
      {section === "reviews" &&
        searchedReviews.map((review) => (
          <div className="order-row" key={review.id}>
            <Avatar name={review.reviewer_name} />
            <div>
              <strong>
                {review.reviewer_name} · {review.rating}/5
              </strong>
              <p style={{ whiteSpace: "normal" }}>
                {review.comment || "No written comment."}
              </p>
            </div>
          </div>
        ))}
    </section>
  );
  return (
    <div className="dashboard seller-dashboard">
      <dialog ref={deleteDialog} className="tm-panel listing-delete-modal" aria-labelledby="delete-title" onCancel={(e) => { e.preventDefault(); if (!busy) setDeleting(null); }}>
        <h2 id="delete-title">Delete this listing?</h2>
        <p>“{deleting?.title}” will be removed along with its photos and verification requests. This cannot be undone.</p>
        {deleteError && <p className="form-error" role="alert">{deleteError}</p>}
        <div className="seller-listing-actions"><button autoFocus className="tm-button secondary" disabled={busy} onClick={() => setDeleting(null)}>Cancel</button><button className="tm-button danger-button" disabled={busy} onClick={deleteListing}>{busy ? 'Deleting…' : 'Delete listing'}</button></div>
      </dialog>
      <aside className="dashboard-sidebar">
        <Brand />
        <div className="sidebar-profile">
          <Avatar name={name} tone="orange" />
          <div>
            <strong>{name}</strong>
            <small>Seller account</small>
            <span className="tm-pill tone-teal">
              Trust score: {user.trust_score}
            </span>
          </div>
        </div>
        <nav className="dashboard-nav">
          {nav.map(([key, label, Icon]) => (
            <button
              className={section === key ? "active" : ""}
              key={key}
              onClick={() =>
                key === "messages" ? navigate("/messages") : setSection(key)
              }
            >
              <Icon />
              {label}
            </button>
          ))}
          <Link to="/profile">
            <Settings />
            Settings
          </Link>
          <Link to="/browse">
            <ShoppingBag />
            Marketplace
          </Link>
        </nav>
        <button
          className="sidebar-logout"
          onClick={() => {
            logout();
            navigate("/login");
          }}
        >
          <LogOut size={15} />
          Log out
        </button>
      </aside>
      <div className="dashboard-main">
        <header className="dashboard-topbar">
          <div>
            <h1>Seller Dashboard</h1>
            <p>Store performance at a glance</p>
          </div>
          <div>
            <Link className="tm-button amber small" to="/seller/new">
              <Plus size={15} /> Add New Listing
            </Link>
            <Link className="icon-button" to="/messages" aria-label="Messages">
              <MessageCircle size={18} />
            </Link>
            <NotificationBell />
            <Link className="header-profile" to="/profile">
              <Avatar name={name} tone="orange" />
              <span>
                {name}
                <small>Seller</small>
              </span>
            </Link>
          </div>
        </header>
        <div className="dashboard-content">
          {notice && <p className="notice" role="status">{notice}</p>}
          {error ? (
            <p role="alert" className="form-error">
              {error}
            </p>
          ) : !data ? (
            <p className="empty-state">Loading your dashboard...</p>
          ) : (
            <>
              <div className="stat-grid">
                <StatCard
                  icon={Banknote}
                  label="Total Revenue"
                  value={money(revenue)}
                  detail="Released earnings after commission"
                  tone="green"
                />
                <StatCard
                  icon={Package}
                  label="Active Listings"
                  value={
                    data.listings.filter((i) => i.status === "verified").length
                  }
                  detail={`${data.listings.filter((i) => i.status === "pending_verification").length} pending review`}
                  tone="blue"
                />
                <StatCard
                  icon={ShoppingBag}
                  label="Orders This Month"
                  value={
                    orders.filter((o) =>
                      o.created_at.startsWith(recentMonths(1)[0].month),
                    ).length
                  }
                  detail={`${orders.length} orders in total`}
                  tone="orange"
                />
                <StatCard
                  icon={Star}
                  label="Average Rating"
                  value={average === "—" ? "No ratings" : `${average} stars`}
                  detail={`${data.reviews.length} reviews`}
                />
              </div>
              {section === "dashboard" && (
                <div className="seller-layout">
                  <div className="dashboard-column">
                    <section className="tm-panel">
                      <div className="dashboard-section-title">
                        <div>
                          <h2>Revenue Overview</h2>
                          <p>Released earnings · Last {months} months</p>
                        </div>
                        <div className="dashboard-tabs">
                          {[6, 3].map((count) => (
                            <button
                              key={count}
                              className={months === count ? "active" : ""}
                              onClick={() => setMonths(count)}
                            >
                              {count} Months
                            </button>
                          ))}
                        </div>
                      </div>
                      <DashboardChart
                        data={monthly}
                        dataKey="amount"
                        color="#f59e0b"
                        pale="#fff1db"
                        money
                      />
                    </section>
                    {renderListings()}
                  </div>
                  <div className="dashboard-column">
                    {renderOrders()}
                    {renderReviews()}
                    <section className="tm-panel">
                      <div className="dashboard-section-title">
                        <h2>Recent Messages</h2>
                        <Link className="text-link" to="/messages">
                          View all
                        </Link>
                      </div>
                      {data.messages.slice(0, 3).map((message) => (
                        <Link
                          to="/messages"
                          className="order-row"
                          key={message.id}
                        >
                          <Avatar name={message.sender_name} />
                          <div>
                            <strong>{message.sender_name}</strong>
                            <p>{message.content}</p>
                          </div>
                        </Link>
                      ))}
                      {!data.messages.length && (
                        <p className="muted">No messages yet.</p>
                      )}
                    </section>
                    <div className="revenue-banner">
                      <div>
                        <p>Expected payout held in escrow</p>
                        <strong>{money(held)}</strong>
                      </div>
                      <button
                        className="tm-button ghost small"
                        onClick={() => setSection("escrow")}
                      >
                        View Escrow
                      </button>
                    </div>
                  </div>
                </div>
              )}
              {section === "listings" && renderListings()}
              {section === "orders" && renderOrders(true)}
              {section === "reviews" && renderReviews()}
              {section === "escrow" && (
                <section className="tm-panel">
                  {orderSearch}
                  <div className="dashboard-section-title">
                    <div>
                      <h2>Escrow & Payouts</h2>
                      <p>
                        Funds are released when the buyer confirms delivery.
                      </p>
                    </div>
                  </div>
                  <div className="dashboard-table-wrap">
                    <table className="dashboard-table">
                      <thead>
                        <tr>
                          <th>Item</th>
                          <th>Order Amount</th>
                          <th>Commission</th>
                          <th>Seller Payout</th>
                          <th>Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {searchedOrders.map((order) => (
                          <tr key={order.id}>
                            <td>{order.listing_title}</td>
                            <td>{money(order.amount)}</td>
                            <td>{money(order.commission_amount)}</td>
                            <td>{money(order.seller_payout)}</td>
                            <td>
                              <StatePill value={order.escrow_status} />
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                    {!orders.length && (
                      <p className="empty-state">No payments yet.</p>
                    )}
                  </div>
                </section>
              )}
              {section === "disputes" && (
                <section className="tm-panel">
                  <div className="dashboard-section-title">
                    <h2>Disputes</h2>
                  </div>
                  {disputeSearch}
                  {searchedDisputes.map((dispute) => (
                    <div className="order-row" key={dispute.id}>
                      <div>
                        <strong>
                          #{dispute.id} · {dispute.listing_title}
                        </strong>
                        <p style={{ whiteSpace: "normal" }}>{dispute.reason}</p>
                        {dispute.resolution_notes && (
                          <p>{dispute.resolution_notes}</p>
                        )}
                      </div>
                      <StatePill value={dispute.status} />
                    </div>
                  ))}
                  {!data.disputes.length && (
                    <p className="muted">No disputes involving your sales.</p>
                  )}
                </section>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
