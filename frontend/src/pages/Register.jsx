// New-account form: collect account details and submit them through the authentication context.
import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { AuthLayout } from '../components/MarketplaceUI';
import AccountFields from '../components/AccountFields';
import PasswordField from '../components/PasswordField';
import { accountError, accountPayload } from '../api/account';

export default function Register() {
  const [params] = useSearchParams();
  const [form, setForm] = useState({ username: '', email: '', password: '', password_confirm: '', first_name: '', last_name: '', phone: '', district: '', nic_number: '', nic_suffix: '', role: params.get('role') === 'seller' ? 'seller' : 'buyer' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [confirmTouched, setConfirmTouched] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();
  const update = (key) => (e) => setForm((old) => ({ ...old, [key]: e.target.value }));
  const mismatch = confirmTouched && form.password !== form.password_confirm;
  // Section: Account creation — check confirmation and submit buyer/seller details.
  const submit = async (e) => {
    e.preventDefault();
    setError(''); setConfirmTouched(true);
    if (form.password !== form.password_confirm) return;
    if (!form.district) { setError('Please select your district.'); return; }
    setSubmitting(true);
    try {
      await register(accountPayload(form));
      navigate(form.role === 'seller' ? '/seller' : '/', { replace: true });
    } catch (err) { setError(accountError(err)); }
    finally { setSubmitting(false); }
  };
  return <AuthLayout register>
    <h1>Create your account</h1>
    <p className="auth-subtitle">Already have an account? <Link to="/login">Sign in →</Link></p>
    <form className="auth-form register-form" onSubmit={submit} onInvalidCapture={() => setError('Please complete all fields correctly.')}>
      <span className="field-label">I want to</span>
      <div className="role-toggle">{['buyer', 'seller'].map((role) => <button type="button" key={role} aria-pressed={form.role === role} className={form.role === role ? 'selected' : ''} onClick={() => setForm((old) => ({ ...old, role }))}>{role === 'buyer' ? 'Buy' : 'Sell'}</button>)}</div>
      <label htmlFor="register-username">Username</label>
      <input id="register-username" className="tm-input" autoComplete="username" required value={form.username} onChange={update('username')} maxLength={150} />
      <AccountFields form={form} setForm={setForm} showHints={false} />
      <PasswordField id="register-password" label="Password" rules showHints={false} autoComplete="new-password" value={form.password} onChange={update('password')} />
      <PasswordField id="register-password-confirm" label="Re-enter password" autoComplete="new-password" value={form.password_confirm} onChange={update('password_confirm')} onBlur={() => setConfirmTouched(true)} aria-invalid={mismatch} aria-describedby={mismatch ? 'confirm-error' : undefined} />
      {mismatch && <p className="form-error" id="confirm-error" role="alert">Passwords do not match.</p>}
      {error && <p className="form-error" role="alert">{error}</p>}
      <button className="tm-button auth-submit" disabled={submitting}>{submitting ? 'Creating account…' : 'Create Account'}</button>
    </form>
    <Link className="auth-back text-link" to="/">← Back to marketplace</Link>
  </AuthLayout>;
}
