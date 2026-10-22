import { Link } from "react-router-dom";
import { Check, ShieldCheck, LockKeyhole, Scale, Award } from "lucide-react";

export function Brand({ light = false }) {
  return (
    <Link to="/" className={`tm-brand ${light ? "light" : ""}`}>
      <span className="tm-logo">
        <Check size={21} strokeWidth={3} />
      </span>
      <span>TrustMart</span>
    </Link>
  );
}
export function Avatar({ name = "?", tone = "teal" }) {
  return (
    <span className={`tm-avatar tone-${tone}`}>
      {name
        .split(/[\s_]+/)
        .map((p) => p[0])
        .join("")
        .slice(0, 2)
        .toUpperCase()}
    </span>
  );
}
const protections = [
  {
    icon: ShieldCheck,
    title: "Product Verification",
    text: "Every listing is reviewed by our team before it goes live.",
    tone: "teal",
  },
  {
    icon: LockKeyhole,
    title: "Escrow Payments",
    text: "Payment is held until you confirm delivery of your item.",
    tone: "orange",
  },
  {
    icon: Scale,
    title: "Dispute Resolution",
    text: "Get support from our moderation team when something goes wrong.",
    tone: "purple",
  },
  {
    icon: Award,
    title: "Trust Badges",
    text: "Make informed choices with seller trust scores and buyer reviews.",
    tone: "green",
  },
];
export function ProtectionGrid() {
  return (
    <div className="protection-grid">
      {protections.map(({ icon: Icon, title, text, tone }) => (
        <div className="protection-item" key={title}>
          <span className={`feature-icon tone-${tone}`}>
            <Icon size={21} />
          </span>
          <div>
            <h3>{title}</h3>
            <p>{text}</p>
          </div>
        </div>
      ))}
    </div>
  );
}
export function AuthLayout({ register = false, children }) {
  return (
    <div className="auth-layout">
      <aside className="auth-story">
        <Brand light />
        <div className="auth-story-copy">
          <h1>
            {register ? (
              <>
                Trade with
                <br />
                confidence.
              </>
            ) : (
              <>
                Welcome
                <br />
                back.
              </>
            )}
          </h1>
          <p>
            {register ? (
              "Join buyers and sellers who transact safely every day in Sri Lanka."
            ) : (
              <>
                Pick up where you left off.
                <br />
                Your listings and chats are waiting.
              </>
            )}
          </p>
          {register && (
            <ul>
              <li>✓ Every listing is manually verified</li>
              <li>✓ Escrow holds your money safely</li>
              <li>✓ Dispute team has your back</li>
            </ul>
          )}
        </div>
        <div className="auth-trust">
          <ShieldCheck size={18} /> A safer way to buy and sell
        </div>
      </aside>
      <section className="auth-main">
        <div className="auth-mobile-brand">
          <Brand />
        </div>
        <div className="auth-form-wrap">{children}</div>
      </section>
    </div>
  );
}
export function Footer() {
  return (
    <footer className="tm-footer">
      <div className="tm-container footer-grid">
        <div>
          <Brand light />
          <p>
            Sri Lanka's verified second-hand marketplace.
            <br />
            Buy and sell with confidence, every time.
          </p>
        </div>
        <div>
          <h3>Marketplace</h3>
          <Link to="/browse">Browse listings</Link>
          <Link to="/categories">Browse categories</Link>
          <Link to="/seller/new">Sell an item</Link>
        </div>
        <div>
          <h3>Trust & support</h3>
          <a href="/#how-it-works">How it works</a>
          <a href="/#safety">Buyer protection</a>
          <Link to="/messages">Messages</Link>
        </div>
        <div>
          <h3>Your account</h3>
          <Link to="/login">Sign in</Link>
          <Link to="/register">Join TrustMart</Link>
          <Link to="/profile">Profile & settings</Link>
        </div>
      </div>
      <div className="tm-container footer-bottom">
        © {new Date().getFullYear()} TrustMart. All rights reserved.
        <span>Made for Sri Lanka</span>
      </div>
    </footer>
  );
}
