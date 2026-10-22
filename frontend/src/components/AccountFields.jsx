import { useId } from 'react';
import LocationSelect from './LocationSelect';

// Section: Shared account fields — contact details, district and seller NIC.
// Section: Seller NIC — require digits of any length; the V/X letter is optional.
export default function AccountFields({ form, setForm, showHints = true, showIdentityHint = true }) {
  const id = useId();
  const update = (key) => (e) => setForm((old) => ({ ...old, [key]: e.target.value }));
  const field = (key, label, props = {}) => <div className="account-field">
    <label htmlFor={`${id}-${key}`}>{label}</label>
    <input id={`${id}-${key}`} className="tm-input" value={form[key]} onChange={update(key)} required {...props} />
  </div>;
  return <>
    <div className="account-field-row">
      {field('first_name', 'First Name', { autoComplete: 'given-name', maxLength: 150 })}
      {field('last_name', 'Last Name', { autoComplete: 'family-name', maxLength: 150 })}
    </div>
    {field('email', 'Email Address', { type: 'email', autoComplete: 'email' })}
    {field('phone', 'Phone Number', { type: 'tel', inputMode: 'numeric', autoComplete: 'tel-national', pattern: '[0-9]{10}', placeholder: '0771234567', title: 'Enter exactly 10 digits, without +, spaces or other characters.', 'aria-describedby': showHints ? `${id}-phone-help` : undefined })}
    {showHints && <small id={`${id}-phone-help`} className="field-help">Exactly 10 digits. Do not use +, spaces or other characters.</small>}
    <LocationSelect district={form.district} districtOnly required onChange={({ district }) => setForm((old) => ({ ...old, district }))} />
    {form.role === 'seller' && <fieldset className="identity-fields">
      <legend>Seller identity</legend>
      <div className="nic-fields">
        {field('nic_number', 'NIC Number', { inputMode: 'numeric', pattern: '[0-9]+', placeholder: 'Enter NIC number', title: 'Enter digits only.', 'aria-describedby': showIdentityHint ? `${id}-nic-help` : undefined })}
        <div className="account-field"><label htmlFor={`${id}-suffix`}>Last letter <span className="optional-label">(optional)</span></label>
          <select id={`${id}-suffix`} className="tm-input" value={form.nic_suffix} onChange={update('nic_suffix')}><option value="">None</option><option>V</option><option>X</option></select>
        </div>
      </div>
      {showIdentityHint && <small id={`${id}-nic-help`} className="field-help">Digits only. Choose None if your NIC has no final letter.</small>}
    </fieldset>}
  </>;
}
