import { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';

// Section: Password input — visibility toggle and optional strength rules.
export default function PasswordField({ id, label, rules = false, showHints = true, ...props }) {
  const [visible, setVisible] = useState(false);
  return <div className="password-field">
    <label htmlFor={id}>{label}</label>
    <div className="password-control">
      <input id={id} className="tm-input" type={visible ? 'text' : 'password'} required
        {...(rules ? { minLength: 8, pattern: '(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9]).{8,}', title: 'Use 8 or more characters with uppercase, lowercase and a number.', 'aria-describedby': showHints ? `${id}-rules` : undefined } : {})} {...props} />
      <button type="button" className="password-toggle" aria-label={`${visible ? 'Hide' : 'Show'} ${label.toLowerCase()}`} aria-pressed={visible} onClick={() => setVisible(!visible)}>
        {visible ? <EyeOff size={19} /> : <Eye size={19} />}
      </button>
    </div>
    {rules && showHints && <small id={`${id}-rules`} className="field-help">At least 8 characters with an uppercase letter (A–Z), a lowercase letter (a–z) and a number (0–9).</small>}
  </div>;
}
