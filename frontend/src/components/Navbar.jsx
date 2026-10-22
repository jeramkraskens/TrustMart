import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { Menu, X, Heart, LogOut, MessageCircle, Settings } from "lucide-react";
import SearchSuggest from './SearchSuggest';
import NotificationBell from './NotificationBell';
import { marketplaceSuggestions } from '../api/search';
import { useAuth } from "../context/AuthContext";
import { Brand, Avatar } from "./MarketplaceUI";

// Section: Navigation — marketplace links, search and account controls.
export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const compact =
    pathname === "/categories" || pathname.startsWith("/listings/");
  const dashboard =
    user?.role === "admin"
      ? "/admin"
      : user?.role === "moderator"
        ? "/moderator"
        : user?.role === "seller"
          ? "/seller"
          : "/buyer";
  const close = () => setOpen(false);
  const submitSearch = (value) => { close(); navigate(value.trim() ? `/browse?search=${encodeURIComponent(value.trim())}` : '/browse'); };
  const searchInput = (label) => <SearchSuggest value={query} onChange={setQuery} label={label}
    placeholder="Search listings, categories or towns…" loadSuggestions={marketplaceSuggestions}
    onSelect={(item) => { close(); navigate(item.url); }} onSearch={submitSearch} />;
  return (
    <header className="tm-header">
      <div className="tm-container tm-header-inner">
        <Brand />
        {compact ? (
          <form
            className="header-search"
            onSubmit={(e) => {
              e.preventDefault();
              submitSearch(query);
            }}
          >
            {searchInput('Search verified listings')}
            <button className="tm-button" type="submit">
              Search
            </button>
          </form>
        ) : (
          <nav className="desktop-nav">
            <Link to="/browse">Browse</Link>
            <Link
              to={
                user?.role === "seller"
                  ? "/seller/new"
                  : "/register?role=seller"
              }
            >
              Sell
            </Link>
            <a href="/#how-it-works">How it Works</a>
            <a href="/#safety">Safety</a>
          </nav>
        )}
        <div className="header-actions">
          {user ? (
            <>
              <Link to="/profile" className="icon-button" aria-label="Profile settings"><Settings size={18} /></Link>
              <NotificationBell />
              <Link
                to="/messages"
                className="icon-button"
                aria-label="Messages"
              >
                <MessageCircle size={18} />
              </Link>
              {user.role === "buyer" && (
                <Link
                  to="/wishlist"
                  className="icon-button"
                  aria-label="Wishlist"
                >
                  <Heart size={18} />
                </Link>
              )}
              <Link className="header-profile" to={dashboard} aria-label="My dashboard">
                <Avatar name={user.first_name || user.username} />
                <span>
                  {user.first_name || user.username}
                  <small>{user.role}</small>
                </span>
              </Link>
              <button
                className="icon-button desktop-logout"
                aria-label="Log out"
                onClick={() => {
                  logout();
                  navigate("/login");
                }}
              >
                <LogOut size={17} />
              </button>
            </>
          ) : (
            <div className="auth-nav">
              <Link className="tm-button secondary small" to="/login">
                Log in
              </Link>
              <Link className="tm-button small" to="/register">
                Sign up
              </Link>
            </div>
          )}
          <button
            className="mobile-menu icon-button"
            aria-label="Toggle navigation"
            aria-expanded={open}
            onClick={() => setOpen(!open)}
          >
            {open ? <X size={20} /> : <Menu size={20} />}
          </button>
        </div>
      </div>
      {pathname !== '/browse' && <div className={`site-search-row ${compact ? 'compact-search-mobile' : ''}`}>
        <form onSubmit={(e) => { e.preventDefault(); submitSearch(query); }}>
          {searchInput('Search marketplace')}
          <button className="tm-button small" type="submit">Search</button>
        </form>
      </div>}
      {open && (
        <nav className="mobile-nav">
          <Link onClick={close} to="/browse">
            Browse listings
          </Link>
          <Link onClick={close} to="/categories">
            Categories
          </Link>
          <Link onClick={close} to="/seller/new">
            Sell an item
          </Link>
          <a onClick={close} href="/#how-it-works">
            How it works
          </a>
          <a onClick={close} href="/#safety">
            Safety
          </a>
          {!user && <Link onClick={close} to="/login">Log in</Link>}
          {user && (
            <>
              <Link onClick={close} to={dashboard}>
                Dashboard
              </Link>
              <Link onClick={close} to="/profile">
                Profile
              </Link>
              <button
                onClick={() => {
                  close();
                  logout();
                  navigate("/login");
                }}
              >
                Log out
              </button>
            </>
          )}
        </nav>
      )}
    </header>
  );
}
