// Public marketplace landing page: introduces the service and displays browsing content.
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowRight, LockKeyhole } from "lucide-react";
import client from "../api/client";
import { getAll } from "../api/marketplace";
import ListingCard from "../components/ListingCard";
import Promotions from '../components/Promotions';
import { Avatar, ProtectionGrid } from "../components/MarketplaceUI";
import { categoryDesigns, categoryHref } from "../api/presentation";
import { useAuth } from "../context/AuthContext";

const steps = [
  ["📋", "List & Verify", "Submit your item. Our team verifies it."],
  ["🔐", "Escrow Payment", "Buyer pays securely. Funds are held in escrow."],
  ["📦", "Deliver Safely", "Arrange delivery and keep your buyer updated."],
  [
    "⭐",
    "Rate & Review",
    "Confirm delivery, release funds and leave a review.",
  ],
];
export default function Home() {
  const { user } = useAuth();
  const [items, setItems] = useState([]);
  const [categories, setCategories] = useState([]);
  const [reviews, setReviews] = useState([]);
  const [recommended, setRecommended] = useState([]);
  const [saved, setSaved] = useState(new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  useEffect(() => {
    const controller = new AbortController();
    Promise.all([
      getAll("/listings/", { status: "verified" }, controller.signal),
      getAll("/categories/", {}, controller.signal),
      getAll("/reviews/", {}, controller.signal),
    ])
      .then(([listings, cats, ratings]) => {
        setItems(listings);
        setCategories(cats);
        setReviews(ratings);
      })
      .catch((e) => {
        if (e.code !== "ERR_CANCELED")
          setError(
            "Could not load the marketplace. Please refresh to try again.",
          );
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, []);
  useEffect(() => {
    if (user?.role !== "buyer") return;
    client
      .get("/recommendations/")
      .then(({ data }) => setRecommended(data.results ?? data))
      .catch(() => {});
    getAll("/wishlist/")
      .then((data) => setSaved(new Set(data.map((w) => w.listing))))
      .catch(() => {});
  }, [user]);
  const onWishlistChange = (id, value) =>
    setSaved((prev) => {
      const next = new Set(prev);
      if (value) next.add(id);
      else next.delete(id);
      return next;
    });
  const rating = reviews.length
    ? (
        reviews.reduce((sum, review) => sum + review.rating, 0) / reviews.length
      ).toFixed(1) + " ★"
    : "New";
  const feature = items.find((item) => /phone/i.test(item.title)) || items[0];
  return (
    <div className="home-page">
      <section className="home-hero">
        <div className="tm-container hero-grid">
          <div className="hero-copy">
            <span className="tm-pill tone-teal">
              <LockKeyhole size={12} /> Verified & Trusted Platform
            </span>
            <h1>
              Buy & Sell
              <br />
              <span>Second-hand Goods</span>
              <br />
              You Can Trust
            </h1>
            <p>
              Sri Lanka's verified marketplace with escrow payments, dispute
              protection, and trust badges.
            </p>
            <div className="button-row">
              <Link className="tm-button" to="/browse">
                Start Shopping <ArrowRight size={16} />
              </Link>
              <Link
                className="tm-button outline"
                to={
                  user?.role === "seller"
                    ? "/seller/new"
                    : "/register?role=seller"
                }
              >
                List an Item
              </Link>
            </div>
          </div>
          <div className="hero-feature">
            {feature ? (
              <ListingCard
                listing={feature}
                featured
                wishlisted={saved.has(feature.id)}
                onWishlistChange={onWishlistChange}
              />
            ) : (
              <div className="hero-placeholder">
                <span>📦</span>
                <h3>A great find starts here</h3>
                <p>Discover verified second-hand goods.</p>
                <Link to="/browse" className="tm-button">
                  Explore listings
                </Link>
              </div>
            )}
          </div>
        </div>
      </section>
      <div className="home-stats">
        <div className="tm-container">
          {[
            [
              loading ? "—" : items.length.toLocaleString(),
              "Verified Listings",
            ],
            ["Verified", "Reviewed by our team"],
            ["Escrow", "Protected payments"],
            [
              rating,
              reviews.length ? "Average Review Rating" : "Growing community",
            ],
          ].map(([value, label]) => (
            <div key={label}>
              <strong>{value}</strong>
              <span>{label}</span>
            </div>
          ))}
        </div>
      </div>
      <Promotions />
      <section className="tm-container home-section" id="how-it-works">
        <div className="section-title centered">
          <h2>How It Works</h2>
        </div>
        <div className="steps-grid">
          {steps.map(([icon, title, text], i) => (
            <div className="step-card" key={title}>
              <span className="step-number">{i + 1}</span>
              <span className="step-emoji">{icon}</span>
              <h3>{title}</h3>
              <p>{text}</p>
            </div>
          ))}
        </div>
      </section>
      <section className="tm-container category-section">
        <div className="section-title centered">
          <h2>Browse by Category</h2>
          <p>Find exactly what you're looking for</p>
        </div>
        <div className="mini-category-grid">
          {categoryDesigns.slice(0, 8).map((cat) => (
            <Link
              to={categoryHref(cat, categories)}
              className="mini-category"
              key={cat.name}
            >
              <span className={`category-icon tone-${cat.tone}`}>
                {cat.icon}
              </span>
              <strong>{cat.short}</strong>
              <small>Explore listings</small>
            </Link>
          ))}
        </div>
        <Link className="text-link category-all" to="/categories">
          View all categories <ArrowRight size={14} />
        </Link>
      </section>
      <section className="featured-section">
        <div className="tm-container">
          <div className="section-title">
            <div>
              <h2>Featured Verified Listings</h2>
              <p>
                Items verified by our team — buy with confidence and escrow
                protection.
              </p>
            </div>
            <Link to="/browse" className="tm-button outline small">
              View all
            </Link>
          </div>
          {error && (
            <p role="alert" className="form-error">
              {error}
            </p>
          )}
          {loading ? (
            <p className="empty-state">Loading verified listings...</p>
          ) : !items.length ? (
            <p className="empty-state">
              No verified listings yet. Check back soon.
            </p>
          ) : (
            <div className="featured-grid">
              {items.slice(0, 4).map((item) => (
                <ListingCard
                  key={item.id}
                  listing={item}
                  featured
                  wishlisted={saved.has(item.id)}
                  onWishlistChange={onWishlistChange}
                />
              ))}
            </div>
          )}
        </div>
      </section>
      {!!recommended.length && (
        <section className="tm-container home-section">
          <div className="section-title">
            <div>
              <span className="tm-pill tone-purple">For you</span>
              <h2>Recommended for you</h2>
              <p>Based on your recent searches and views.</p>
            </div>
          </div>
          <div className="featured-grid">
            {recommended.slice(0, 4).map((item) => (
              <ListingCard
                key={item.id}
                listing={item}
                wishlisted={saved.has(item.id)}
                onWishlistChange={onWishlistChange}
              />
            ))}
          </div>
        </section>
      )}
      <section className="tm-container safety-section" id="safety">
        <div className="safety-copy">
          <small>WHY TRUSTMART</small>
          <h2>
            Built for trust,
            <br />
            not just transactions.
          </h2>
          <p>
            Every feature we built started with one question:
            <br />
            How can we make the buyer feel safe? From verification to escrow,
            we've got you covered.
          </p>
          <Link to="/browse" className="tm-button">
            Explore the marketplace
          </Link>
        </div>
        <ProtectionGrid />
      </section>
      <section className="reviews-section">
        <div className="tm-container">
          <div className="section-title centered">
            <h2>What our users say</h2>
            <p>Reviews from buyers and sellers in our community</p>
          </div>
          {reviews.length ? (
            <div className="home-review-grid">
              {reviews.slice(0, 3).map((review) => (
                <article className="quote-card" key={review.id}>
                  <span className="quote-mark">“</span>
                  <p>{review.comment || "A review from a completed trade."}</p>
                  <div className="review-person">
                    <Avatar name={review.reviewer_name} />
                    <div>
                      <strong>{review.reviewer_name}</strong>
                      <span className="stars">{"★".repeat(review.rating)}</span>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          ) : (
            <div className="empty-state">
              Your next great trade could be our first review. Join the
              community and share your experience.
            </div>
          )}
        </div>
      </section>
      <section className="join-banner">
        <h2>Ready to buy or sell safely?</h2>
        <p>Trade with confidence on TrustMart.</p>
        <div className="button-row">
          <Link className="tm-button white" to="/register">
            Join TrustMart Free
          </Link>
          <Link className="tm-button ghost" to="/browse">
            Browse Listings
          </Link>
        </div>
      </section>
    </div>
  );
}
