// Login form: collect credentials and use the shared authentication context to sign in.
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Eye, EyeOff, ShieldCheck } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { AuthLayout } from "../components/MarketplaceUI";

export default function Login() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [visible, setVisible] = useState(false);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();
  const submit = async (e) => {
    e.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      const user = await login(username, password);
      navigate(
        user.role === "admin"
          ? "/admin"
          : user.role === "seller"
            ? "/seller"
            : user.role === "moderator"
              ? "/moderator"
              : "/",
        { replace: true },
      );
    } catch {
      setError("Invalid username or password. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };
  return (
    <AuthLayout>
      <h1>Sign in to TrustMart</h1>
      <p className="auth-subtitle">
        New to TrustMart? <Link to="/register">Create an account →</Link>
      </p>
      <form className="auth-form" onSubmit={submit}>
        <label htmlFor="login-username">Username</label>
        <input
          id="login-username"
          className="tm-input"
          autoComplete="username"
          placeholder="Your username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          required
        />
        <label htmlFor="login-password">Password</label>
        <div className="password-input">
          <input
            id="login-password"
            className="tm-input"
            type={visible ? "text" : "password"}
            autoComplete="current-password"
            placeholder="Enter your password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          <button
            type="button"
            aria-label={visible ? "Hide password" : "Show password"}
            onClick={() => setVisible(!visible)}
          >
            {visible ? <EyeOff size={18} /> : <Eye size={18} />}
          </button>
        </div>
        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}
        <button className="tm-button auth-submit" disabled={submitting}>
          {submitting ? "Signing in..." : "Sign In"}
        </button>
      </form>
      <div className="auth-divider">
        <span>A marketplace built on trust</span>
      </div>
      <p className="auth-assurance">
        <ShieldCheck size={19} /> Verified listings. Protected payments. Safer
        trading.
      </p>
      <Link className="auth-back text-link" to="/">
        Continue as guest →
      </Link>
    </AuthLayout>
  );
}
