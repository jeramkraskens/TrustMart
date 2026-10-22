// Product browsing screen. Search and filter choices determine which listings are displayed.
import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { SlidersHorizontal, X } from "lucide-react";
import SearchSuggest from '../components/SearchSuggest';
import { marketplaceSuggestions, matchesSearch, recordSuggestions } from '../api/search';
import client from "../api/client";
import { getAll } from "../api/marketplace";
import { useAuth } from "../context/AuthContext";
import ListingCard from "../components/ListingCard";
import LocationSelect from "../components/LocationSelect";
import { categoryDesigns, categoryHref, conditionLabel } from "../api/presentation";

export default function Browse() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const search = params.get("search") || "";
  const category = params.get("category") || "";
  const [query, setQuery] = useState(search);
  const [items, setItems] = useState([]);
  const [categories, setCategories] = useState([]);
  const [saved, setSaved] = useState(new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [sort, setSort] = useState("-created_at");
  const [page, setPage] = useState(1);
  const [showFilters, setShowFilters] = useState(false);
  // Section: Browse filters — keep URL selections and form filters in sync.
  const emptyFilters = { min: "", max: "", conditions: [], location: "", district: "", city: "" };
  const urlLocation = { district: params.get('district') || '', city: params.get('city') || '' };
  const [draft, setDraft] = useState({ ...emptyFilters, ...urlLocation });
  const [filters, setFilters] = useState({ ...emptyFilters, ...urlLocation });
  const queryKey = `${search}|${category}|${urlLocation.district}|${urlLocation.city}`;
  const [previousQueryKey, setPreviousQueryKey] = useState(queryKey);
  if (queryKey !== previousQueryKey) {
    setPreviousQueryKey(queryKey);
    setQuery(search);
    setDraft((old) => ({ ...old, ...urlLocation }));
    setFilters((old) => ({ ...old, ...urlLocation }));
    setPage(1);
  }
  useEffect(() => {
    Promise.all([
      getAll("/listings/", { status: "verified" }),
      getAll("/categories/"),
    ])
      .then(([listings, cats]) => {
        setItems(listings);
        setCategories(cats);
      })
      .catch(() =>
        setError("Unable to load listings. Please refresh to try again."),
      )
      .finally(() => setLoading(false));
  }, []);
  useEffect(() => {
    if (user?.role !== "buyer") return;
    getAll("/wishlist/")
      .then((data) => setSaved(new Set(data.map((w) => w.listing))))
      .catch(() => {});
    if (search.trim())
      client
        .post("/recommendations/log-search/", { query: search.trim() })
        .catch(() => {});
  }, [user, search]);
  // Section: Results — apply search, price, condition and location filters.
  const filtered = useMemo(
    () =>
      items
        .filter((item) => {
          return (
            (!category || String(item.category) === category) &&
            matchesSearch(item, ['title', 'description'], search) &&
            (filters.min === "" || Number(item.price) >= Number(filters.min)) &&
            (filters.max === "" || Number(item.price) <= Number(filters.max)) &&
            (!filters.conditions.length ||
              filters.conditions.includes(item.condition)) &&
            (!filters.district || item.district === filters.district) &&
            (!filters.city || item.city === filters.city) &&
            (!filters.location ||
              item.location
                .toLowerCase()
                .includes(filters.location.toLowerCase()))
          );
        })
        .sort((a, b) =>
          sort === "price"
            ? Number(a.price) - Number(b.price)
            : sort === "-price"
              ? Number(b.price) - Number(a.price)
              : new Date(b.created_at) - new Date(a.created_at),
        ),
    [items, category, search, filters, sort],
  );
  const pages = Math.max(1, Math.ceil(filtered.length / 9));
  const currentPage = Math.min(page, pages);
  const visible = filtered.slice((currentPage - 1) * 9, currentPage * 9);
  const changeFilter = (key, value) =>
    setDraft((old) => ({ ...old, [key]: value }));
  const reset = () => {
    setDraft(emptyFilters);
    setFilters(emptyFilters);
    setParams({});
    setPage(1);
  };
  const submitSearch = (value) => {
    const next = new URLSearchParams(params);
    if (value.trim()) next.set('search', value.trim());
    else next.delete('search');
    setParams(next);
  };
  return (
    <div className="browse-page">
      <div className="page-heading">
        <div className="tm-container">
          <div>
            <h1>Browse Verified Listings</h1>
            {!user && <p>Browse verified items without an account. Sign in when you want to buy or save an item.</p>}
            <p>
              {loading
                ? "Loading listings..."
                : `${items.length.toLocaleString()} listings available • All verified by our team`}
            </p>
          </div>
          <select
            aria-label="Sort listings"
            className="tm-input sort-select"
            value={sort}
            onChange={(e) => {
              setSort(e.target.value);
              setPage(1);
            }}
          >
            <option value="-created_at">Sort: Most Recent</option>
            <option value="price">Price: Low to High</option>
            <option value="-price">Price: High to Low</option>
          </select>
        </div>
      </div>
      <div className="tm-container browse-content">
        <form
          className="browse-search"
          onSubmit={(e) => {
            e.preventDefault();
            submitSearch(query);
          }}
        >
          <SearchSuggest label="Search listings" placeholder="Search for phones, laptops, vehicles…" value={query} onChange={setQuery}
            loadSuggestions={marketplaceSuggestions} onSelect={(item) => navigate(item.url)} onSearch={submitSearch} />
          <button className="tm-button" type="submit">
            Search
          </button>
        </form>
        <div className="category-chips">
          <Link
            className={`search-chip ${!category && !search ? "active" : ""}`}
            to="/browse"
          >
            ⊞ All
          </Link>
          {categoryDesigns.slice(0, 8).map((cat) => (
            <Link
              className={`search-chip ${categoryHref(cat, categories) === "/browse?" + params.toString() ? "active" : ""}`}
              key={cat.name}
              to={categoryHref(cat, categories)}
            >
              {cat.icon} {cat.short}
            </Link>
          ))}
          <Link className="text-link" to="/categories">
            All categories →
          </Link>
        </div>
        <button
          className="tm-button secondary filter-toggle"
          onClick={() => setShowFilters(!showFilters)}
        >
          <SlidersHorizontal size={17} /> Filters
        </button>
        <div className="browse-grid">
          <form
            className={`filter-panel tm-panel ${showFilters ? "is-open" : ""}`}
            onSubmit={(e) => {
              e.preventDefault();
              setFilters({ ...draft });
              setPage(1);
              setShowFilters(false);
            }}
          >
            <div className="filter-heading">
              <h2>Filters</h2>
              <button type="button" className="text-link" onClick={reset}>
                Reset
              </button>
            </div>
            <div className="filter-section">
              <span className="muted">Active Filters</span>
              <div className="active-filters">
                <span className="tm-pill tone-teal">Verified Only</span>
                {search && (
                  <button
                    type="button"
                    className="tm-pill tone-teal"
                    onClick={() => {
                      const next = new URLSearchParams(params);
                      next.delete("search");
                      setParams(next);
                    }}
                  >
                    {search} <X size={11} />
                  </button>
                )}
              </div>
            </div>
            <div className="filter-section">
              <h3>Price Range</h3>
              <p>Set your budget in rupees</p>
              <div className="price-inputs">
                <input
                  className="tm-input"
                  type="number"
                  min="0"
                  aria-label="Minimum price"
                  placeholder="Min Rs."
                  value={draft.min}
                  onChange={(e) => changeFilter("min", e.target.value)}
                />
                <span>–</span>
                <input
                  className="tm-input"
                  type="number"
                  min={draft.min || 0}
                  aria-label="Maximum price"
                  placeholder="Max Rs."
                  value={draft.max}
                  onChange={(e) => changeFilter("max", e.target.value)}
                />
              </div>
            </div>
            <div className="filter-section">
              <h3>Condition</h3>
              {["new", "excellent", "good", "fair"].map((condition) => (
                <label className="check-row" key={condition}>
                  <input
                    type="checkbox"
                    checked={draft.conditions.includes(condition)}
                    onChange={(e) =>
                      changeFilter(
                        "conditions",
                        e.target.checked
                          ? [...draft.conditions, condition]
                          : draft.conditions.filter((c) => c !== condition),
                      )
                    }
                  />
                  <span>{conditionLabel(condition)}</span>
                  <small>
                    {items.filter((i) => i.condition === condition).length}
                  </small>
                </label>
              ))}
            </div>
            <div className="filter-section">
              <h3>Trust Level</h3>
              <p className="trust-note">
                ✓ Every item here is verified by our moderation team.
              </p>
            </div>
            <div className="filter-section">
              <h3>Location</h3>
              <LocationSelect district={draft.district} city={draft.city} onChange={(value) => setDraft((old) => ({ ...old, ...value, location: '' }))} />
              <div className="record-search"><SearchSuggest label="City or district" placeholder="Search older location text…"
                value={draft.location} onChange={(value) => changeFilter('location', value)}
                suggestions={recordSuggestions(items, ['location'], draft.location)} /></div>
            </div>
            <button className="tm-button full-width">Apply Filters</button>
          </form>
          <div>
            {error ? (
              <div role="alert" className="empty-state form-error">
                {error}
              </div>
            ) : loading ? (
              <div className="empty-state">Loading listings...</div>
            ) : visible.length ? (
              <div className="browse-listings">
                {visible.map((item) => (
                  <ListingCard
                    key={item.id}
                    listing={item}
                    wishlisted={saved.has(item.id)}
                    onWishlistChange={(id, value) =>
                      setSaved((old) => {
                        const next = new Set(old);
                        if (value) next.add(id);
                        else next.delete(id);
                        return next;
                      })
                    }
                  />
                ))}
              </div>
            ) : (
              <div className="empty-state tm-panel">
                <h2>No listings found</h2>
                <p>Try another search or adjust your filters.</p>
                <button className="tm-button" onClick={reset}>
                  Clear filters
                </button>
              </div>
            )}
            <div className="pagination-row">
              <span>
                {filtered.length
                  ? `Showing ${(currentPage - 1) * 9 + 1}–${Math.min(currentPage * 9, filtered.length)} of ${filtered.length} results`
                  : "0 results"}
              </span>
              <nav className="pagination" aria-label="Listing pages">
                <button
                  disabled={currentPage <= 1}
                  onClick={() => setPage(currentPage - 1)}
                >
                  ‹ Prev
                </button>
                {Array.from({ length: pages }, (_, i) => i + 1)
                  .filter(
                    (p) =>
                      p === 1 || p === pages || Math.abs(p - currentPage) <= 1,
                  )
                  .map((p) => (
                    <button
                      key={p}
                      className={currentPage === p ? "active" : ""}
                      aria-current={currentPage === p ? "page" : undefined}
                      onClick={() => setPage(p)}
                    >
                      {p}
                    </button>
                  ))}
                <button
                  disabled={currentPage >= pages}
                  onClick={() => setPage(currentPage + 1)}
                >
                  Next ›
                </button>
              </nav>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
