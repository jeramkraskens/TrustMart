import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Heart } from "lucide-react";
import client, { mediaUrl } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { Avatar } from "./MarketplaceUI";
import { conditionLabel, itemEmoji, money } from "../api/presentation";

export default function ListingCard({
  listing,
  wishlisted = false,
  onWishlistChange,
  featured = false,
}) {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [saved, setSaved] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const isSaved = saved ?? wishlisted;
  const toggle = async () => {
    if (!user) return navigate("/login");
    setSaving(true);
    try {
      const { data } = await client.post("/wishlist/toggle/", {
        listing: listing.id,
      });
      setSaved(data.wishlisted);
      onWishlistChange?.(listing.id, data.wishlisted);
    } catch {
      setError("Could not update wishlist. Please try again.");
    } finally {
      setSaving(false);
    }
  };
  return (
    <article className="listing-card">
      <Link
        to={`/listings/${listing.id}`}
        className="listing-image"
        aria-label={`View ${listing.title}`}
      >
        {listing.images?.[0]?.image ? (
          <img
            src={mediaUrl(listing.images[0].image)}
            alt={listing.title}
            loading="lazy"
          />
        ) : (
          <span className="product-emoji">{itemEmoji(listing)}</span>
        )}
        <div className="listing-image-meta">
          <span className="tm-pill tone-teal">
            {listing.status === "verified"
              ? "Verified"
              : listing.status.replaceAll("_", " ")}
          </span>
          <span className="condition-label">
            {conditionLabel(listing.condition)}
          </span>
        </div>
      </Link>
      <div className="listing-body">
        <Link to={`/listings/${listing.id}`} className="listing-title">
          {listing.title}
        </Link>
        <p className="listing-price">{money(listing.price)}</p>
        <p className="listing-location">
          {listing.location || listing.category_name}
        </p>
        <div className="listing-seller">
          <span>
            <Avatar name={listing.seller_name} />
            {listing.seller_name}
          </span>
          {(!user || user.role === "buyer") && (
            <button
              className={`wishlist-button ${isSaved ? "saved" : ""}`}
              disabled={saving}
              onClick={toggle}
              aria-label={`${isSaved ? "Remove" : "Save"} ${listing.title} ${isSaved ? "from" : "to"} wishlist`}
            >
              <Heart size={16} fill={isSaved ? "currentColor" : "none"} />
            </button>
          )}
        </div>
        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}
        {featured && (
          <Link
            className="tm-button card-button"
            to={`/listings/${listing.id}`}
          >
            View and Buy Safely
          </Link>
        )}
      </div>
    </article>
  );
}
