import { NavLink, Link, Route, Routes, useNavigate } from "react-router-dom";
import {
  LayoutDashboard,
  Users,
  ListChecks,
  ShieldCheck,
  Tags,
  Megaphone,
  Receipt,
  Wallet,
  Gavel,
  Star,
  Sparkles,
  MessageCircle,
  LogOut,
  ArrowLeft,
  FileDown,
} from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import { useState } from 'react';
import SearchSuggest from '../../components/SearchSuggest';
import { Avatar, Brand } from "../../components/MarketplaceUI";
import AdminOverview from "./AdminOverview";
import AdminUsers from "./AdminUsers";
import AdminListings from "./AdminListings";
import AdminCategories from "./AdminCategories";
import AdminTransactions from "./AdminTransactions";
import AdminDisputes from "./AdminDisputes";
import AdminVerifications from "./AdminVerifications";
import AdminReviews from "./AdminReviews";
import AdminRevenue from "./AdminRevenue";
import AdminRecommendations from "./AdminRecommendations";
import AdminAdvertisements from "./AdminAdvertisements";
import AdminReports from "./AdminReports";
import NotificationBell from '../../components/NotificationBell';

// Section: Admin navigation — map sidebar entries to management pages.
const nav = [
  ["", "Overview", LayoutDashboard, AdminOverview],
  ["users", "Users", Users, AdminUsers],
  ["listings", "Listings", ListChecks, AdminListings],
  ["transactions", "Orders & Escrow", Receipt, AdminTransactions],
  ["disputes", "Disputes", Gavel, AdminDisputes],
  ["verifications", "Verification", ShieldCheck, AdminVerifications],
  ["reports", "Reports", FileDown, AdminReports],
  ["revenue", "Revenue", Wallet, AdminRevenue],
  ["categories", "Categories", Tags, AdminCategories],
  ["reviews", "Reviews", Star, AdminReviews],
  ["advertisements", "Advertisements", Megaphone, AdminAdvertisements],
  ["recommendations", "ML Recommender", Sparkles, AdminRecommendations],
];
export default function AdminLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [sectionSearch, setSectionSearch] = useState('');
  return (
    <div className="dashboard admin-dashboard">
      <aside className="dashboard-sidebar">
        <Brand light />
        <div className="sidebar-profile">
          <Avatar name={user.username} tone="purple" />
          <div>
            <strong>{user.username}</strong>
            <small>Administrator</small>
            <span className="tm-pill tone-green">● Online</span>
          </div>
        </div>
        <nav className="dashboard-nav">
          {nav.map(([path, label, Icon]) => (
            <NavLink
              key={path}
              to={`/admin${path ? "/" + path : ""}`}
              end={!path}
              className={({ isActive }) => (isActive ? "active" : "")}
            >
              <Icon />
              {label}
            </NavLink>
          ))}
          <Link to="/browse">
            <ArrowLeft />
            Back to marketplace
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
            <h1>Platform Overview</h1>
            <p>Live data across the entire TrustMart platform</p>
          </div>
          <div>
            <span className="admin-date">
              {new Date().toLocaleDateString("en-GB", {
                day: "numeric",
                month: "short",
                year: "numeric",
              })}
            </span>
            <Link className="tm-button navy small" to="/admin/reports">
              Export Reports
            </Link>
            <Link className="icon-button" to="/messages" aria-label="Messages">
              <MessageCircle size={17} />
            </Link>
            <NotificationBell />
            <Link className="header-profile" to="/profile">
              <Avatar name={user.username} tone="purple" />
              <span>
                {user.username}
                <small>Administrator</small>
              </span>
            </Link>
          </div>
        </header>
        <div className="admin-content">
          <div className="admin-search-navigation"><SearchSuggest value={sectionSearch} onChange={setSectionSearch}
            label="Find admin section" placeholder="Find users, listings, advertisements…"
            suggestions={nav.filter(([, name]) => name.toLowerCase().includes(sectionSearch.toLowerCase().trim())).map(([path, name]) => ({ id: path || 'overview', label: name, value: name, detail: 'Admin section', url: `/admin${path ? '/' + path : ''}` }))}
            onSelect={(item) => { navigate(item.url); setSectionSearch(''); }} /></div>
          <Routes>
            {nav.map(([path, , , Component]) =>
              path ? (
                <Route key={path} path={path} element={<Component />} />
              ) : (
                <Route key="overview" index element={<Component />} />
              ),
            )}
          </Routes>
        </div>
      </div>
    </div>
  );
}
