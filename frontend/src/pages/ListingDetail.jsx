// Individual product screen with seller information and available buyer actions.
import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { Heart, LockKeyhole, MessageCircle, Phone } from "lucide-react";
import client, { mediaUrl } from "../api/client";
import { getAll } from "../api/marketplace";
import { useAuth } from "../context/AuthContext";
import { Avatar, ProtectionGrid } from "../components/MarketplaceUI";
import { conditionLabel, itemEmoji, money } from "../api/presentation";
import ListingCard from "../components/ListingCard";
import EscrowCheckoutModal from "../components/EscrowCheckoutModal";

export default function ListingDetail() {
  const { id } = useParams();
  return <ListingDetailContent key={id} id={id} />;
}

function ListingDetailContent({ id }) {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [listing, setListing] = useState(null);
  const [activeImage, setActiveImage] = useState(0);
  const [showCheckout, setShowCheckout] = useState(false);
  const [message, setMessage] = useState("");
  const [sending, setSending] = useState(false);
  const [notice, setNotice] = useState("");
  const [wishlisted, setWishlisted] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [reviews, setReviews] = useState([]);
  const [similar, setSimilar] = useState([]);
  const [reviewError, setReviewError] = useState(false);
  const [showMessage, setShowMessage] = useState(false);
  const [allReviews, setAllReviews] = useState(false);
  const messageRef = useRef(null);
  useEffect(() => {
    const controller = new AbortController();
    client
      .get(`/listings/${id}/`, { signal: controller.signal })
      .then(({ data }) => {
        setListing(data);
        getAll("/reviews/", { reviewee: data.seller }, controller.signal)
          .then(setReviews)
          .catch((e) => {
            if (e.code !== "ERR_CANCELED") setReviewError(true);
          });
        client
          .get("/listings/", {
            params: { status: "verified", category: data.category },
            signal: controller.signal,
          })
          .then(({ data: listings }) =>
            setSimilar(
              (listings.results ?? listings)
                .filter((item) => String(item.id) !== id)
                .slice(0, 4),
            ),
          )
          .catch(() => {});
      })
      .catch((e) => {
        if (e.code !== "ERR_CANCELED")
          setError(
            e.response?.status === 404
              ? "Listing not found. It may have been removed."
              : "Could not load this listing. Please try again.",
          );
      });
    return () => controller.abort();
  }, [id]);
  useEffect(() => {
    if (user?.role !== "buyer") return;
    getAll("/wishlist/")
      .then((items) =>
        setWishlisted(items.some((w) => String(w.listing) === id)),
      )
      .catch(() => {});
    client.post("/recommendations/log-view/", { listing: id }).catch(() => {});
  }, [id, user]);
  useEffect(() => {
    if (showMessage) messageRef.current?.focus();
  }, [showMessage]);
  if (error)
    return (
      <div className="tm-container empty-state" role="alert">
        <h1>{error}</h1>
        <Link className="tm-button" to="/browse">
          Back to listings
        </Link>
      </div>
    );
  if (!listing)
    return <div className="tm-container empty-state">Loading listing...</div>;
  const toggleWishlist = async () => {
    if (!user) return navigate("/login");
    setSaving(true);
    try {
      const { data } = await client.post("/wishlist/toggle/", { listing: id });
      setWishlisted(data.wishlisted);
    } catch {
      setNotice("Could not update your wishlist. Please try again.");
    } finally {
      setSaving(false);
    }
  };
  const openMessage = (offer = false) => {
    if (!user) return navigate("/login");
    setShowMessage(true);
    if (offer)
      setMessage(
        `Hi, I would like to make an offer for ${listing.title}: Rs. `,
      );
  };
  const sendMessage = async (e) => {
    e.preventDefault();
    if (!message.trim()) return;
    setSending(true);
    try {
      await client.post("/messages/", {
        receiver: listing.seller,
        listing: listing.id,
        content: message.trim(),
      });
      setMessage("");
      setNotice(
        "Message sent to the seller. Continue your conversation in Messages.",
      );
    } catch {
      setNotice("Could not send your message. Please try again.");
    } finally {
      setSending(false);
    }
  };
  const images = listing.images || [];
  const rating = reviews.length
    ? (reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length).toFixed(
        1,
      )
    : null;
  const canContact = !user || user.id !== listing.seller;
  return (
    <div className="tm-container detail-container">
      <nav className="breadcrumbs" aria-label="Breadcrumb">
        <Link to="/">Home</Link>
        <span>/</span>
        <Link to={`/browse?category=${listing.category || ""}`}>
          {listing.category_name || "Listings"}
        </Link>
        <span>/</span>
        <span>{listing.title}</span>
      </nav>
      <div className="detail-grid">
        <div className="detail-column">
          <div className="product-gallery">
            <div className="product-main-image">
              <span className="tm-pill">
                {listing.status === "verified"
                  ? "● Verified Listing"
                  : listing.status.replaceAll("_", " ")}
              </span>
              {images.length ? (
                <img
                  src={mediaUrl(images[activeImage]?.image)}
                  alt={listing.title}
                />
              ) : (
                <span className="product-emoji">{itemEmoji(listing)}</span>
              )}
            </div>
            {images.length > 1 ? (
              <div className="gallery-thumbs">
                {images.map((image, i) => (
                  <button
                    key={image.id}
                    aria-label={`View photo ${i + 1}`}
                    aria-pressed={activeImage === i}
                    className={i === activeImage ? "active" : ""}
                    onClick={() => setActiveImage(i)}
                  >
                    <img
                      src={mediaUrl(image.image)}
                      alt={`${listing.title}, photo ${i + 1}`}
                    />
                  </button>
                ))}
              </div>
            ) : (
              <p className="gallery-caption">
                {images.length
                  ? "Product photo submitted by the seller"
                  : "The seller has not added photos yet"}
              </p>
            )}
          </div>
          <div className="detail-tags">
            <span className="tm-pill tone-green">
              {conditionLabel(listing.condition)} Condition
            </span>
            {listing.status === "verified" && (
              <span className="tm-pill tone-teal">Moderator Verified</span>
            )}
            <span className="tm-pill tone-orange">Escrow Payments</span>
          </div>
          <section className="tm-panel detail-description">
            <h2>Description</h2>
            <p>{listing.description}</p>
          </section>
          <section className="tm-panel detail-specs">
            <h2>Item Details</h2>
            <dl>
              <dt>Category</dt>
              <dd>{listing.category_name || "Uncategorized"}</dd>
              <dt>Condition</dt>
              <dd>{conditionLabel(listing.condition)}</dd>
              <dt>Location</dt>
              <dd>{listing.location || "Not specified"}</dd>
              <dt>Listed on</dt>
              <dd>{new Date(listing.created_at).toLocaleDateString()}</dd>
            </dl>
          </section>
        </div>
        <div className="detail-column">
          <section className="tm-panel purchase-panel">
            <h1>{listing.title}</h1>
            <div className="product-rating">
              {rating ? (
                <>
                  <span className="stars">
                    {"★".repeat(Math.round(Number(rating)))}
                  </span>
                  <strong>{rating}</strong>
                  <a href="#seller-reviews">
                    ({reviews.length} seller reviews)
                  </a>
                </>
              ) : (
                <span>No seller reviews yet</span>
              )}
            </div>
            <p className="detail-price">{money(listing.price)}</p>
            <div className="escrow-notice">
              <LockKeyhole size={22} />
              <div>
                <strong>Escrow Payment Protected</strong>
                <p>Your payment is held until you confirm delivery.</p>
              </div>
            </div>
            {listing.status === "verified" &&
              (!user || user.role === "buyer") && (
                <button
                  className="tm-button"
                  onClick={() =>
                    user ? setShowCheckout(true) : navigate("/login")
                  }
                >
                  Buy Safely with Escrow
                </button>
              )}
            {canContact && (
              <button
                className="tm-button outline"
                onClick={() => openMessage(true)}
              >
                Make an Offer
              </button>
            )}
            <div className="detail-actions">
              {canContact && listing.contact_phone && <>
                <a className="tm-button outline" href={`tel:${listing.contact_phone}`}><Phone size={16} />Call {listing.contact_phone}</a>
                {listing.whatsapp_enabled && <a className="tm-button whatsapp-button" target="_blank" rel="noopener noreferrer" href={`https://wa.me/${listing.contact_phone.replace(/\D/g, '').replace(/^0/, '94')}?text=${encodeURIComponent(`Hi, I'm interested in ${listing.title} on TrustMart. ${window.location.origin}/listings/${listing.id}`)}`}><MessageCircle size={16} />WhatsApp seller</a>}
              </>}
              {canContact && (
                <button
                  className="tm-button secondary"
                  onClick={() => openMessage()}
                >
                  <MessageCircle size={15} /> Chat with Seller
                </button>
              )}
              {(!user || user.role === "buyer") && (
                <button
                  className="tm-button secondary"
                  onClick={toggleWishlist}
                  disabled={saving}
                >
                  <Heart
                    size={15}
                    fill={wishlisted ? "currentColor" : "none"}
                  />
                  {wishlisted ? "Saved to Wishlist" : "Save to Wishlist"}
                </button>
              )}
            </div>
            {showMessage && (
              <form onSubmit={sendMessage} className="message-form">
                <label htmlFor="seller-message">Your message</label>
                <textarea
                  ref={messageRef}
                  id="seller-message"
                  className="tm-input"
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  placeholder="Ask the seller a question..."
                  required
                />
                <button className="tm-button small" disabled={sending}>
                  {sending ? "Sending..." : "Send message"}
                </button>
              </form>
            )}
            {notice && (
              <p className="notice" role="status">
                {notice}
              </p>
            )}
          </section>
          <section className="tm-panel seller-panel">
            <h2>About the Seller</h2>
            <div className="seller-identity">
              <Avatar name={listing.seller_name} />
              <div>
                <h3>{listing.seller_name}</h3>
                <p>{listing.location || "Sri Lanka"}</p>
                <span className="tm-pill tone-teal">
                  Trust score: {listing.seller_trust_score ?? "—"}
                </span>
              </div>
            </div>
            <div className="seller-metrics">
              <div>
                <strong>{rating || "—"}</strong>
                <span>Average rating</span>
              </div>
              <div>
                <strong>{reviews.length}</strong>
                <span>Seller reviews</span>
              </div>
            </div>
          </section>
          <section className="tm-panel buyer-protection">
            <h2>Buyer Protection</h2>
            <ProtectionGrid />
          </section>
        </div>
      </div>
      <section className="tm-panel detail-reviews" id="seller-reviews">
        <div className="section-title">
          <h2>Seller Reviews ({reviews.length})</h2>
          {reviews.length > 3 && (
            <button
              className="text-link"
              onClick={() => setAllReviews(!allReviews)}
            >
              {allReviews ? "Show less" : "View all →"}
            </button>
          )}
        </div>
        {reviewError ? (
          <p className="muted">Reviews are temporarily unavailable.</p>
        ) : reviews.length ? (
          <div className="detail-review-grid">
            <div className="rating-summary">
              <strong>{rating}</strong>
              <span className="stars">★★★★★</span>
              <p>{reviews.length} reviews</p>
            </div>
            {reviews.slice(0, allReviews ? undefined : 3).map((review) => (
              <article className="review-card" key={review.id}>
                <div className="review-person">
                  <Avatar name={review.reviewer_name} />
                  <div>
                    <strong>{review.reviewer_name}</strong>
                    <span className="stars">{"★".repeat(review.rating)}</span>
                  </div>
                </div>
                <small className="muted">
                  {new Date(review.created_at).toLocaleDateString()}
                </small>
                <p>{review.comment || "No written comment."}</p>
              </article>
            ))}
          </div>
        ) : (
          <p className="muted">
            No reviews yet. Reviews from this seller's buyers will appear here.
          </p>
        )}
      </section>
      {!!similar.length && (
        <section>
          <div className="section-title">
            <h2>Similar Verified Listings</h2>
            <Link
              className="text-link"
              to={`/browse?category=${listing.category || ""}`}
            >
              View all →
            </Link>
          </div>
          <div className="featured-grid">
            {similar.map((item) => (
              <ListingCard key={item.id} listing={item} featured />
            ))}
          </div>
        </section>
      )}
      {showCheckout && (
        <EscrowCheckoutModal
          listing={listing}
          onClose={() => setShowCheckout(false)}
          onSuccess={() => {
            setShowCheckout(false);
            setNotice(
              "Purchase complete! Payment is held in escrow until you confirm delivery. View it in My purchases.",
            );
            client
              .get(`/listings/${id}/`)
              .then(({ data }) => setListing(data))
              .catch(() => {});
          }}
        />
      )}
    </div>
  );
}
