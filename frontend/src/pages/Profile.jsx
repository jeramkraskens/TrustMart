// Signed-in account screen for viewing and updating profile information.
import { useState } from 'react';
import client from '../api/client';
import { useAuth } from '../context/AuthContext';
import AccountFields from '../components/AccountFields';
import PasswordField from '../components/PasswordField';
import { accountError, accountPayload } from '../api/account';

export default function Profile() {
  const { user, refreshUser } = useAuth();
  const [form, setForm] = useState({ first_name: user.first_name || '', last_name: user.last_name || '', email: user.email || '', phone: user.phone || '', district: user.district || '', role: user.role, nic_number: user.nic_number || '', nic_suffix: user.nic_suffix || '' });
  const [profileSaving, setProfileSaving] = useState(false);
  const [profileMsg, setProfileMsg] = useState('');
  const [profileError, setProfileError] = useState('');
  const [pwForm, setPwForm] = useState({ old_password: '', new_password: '', password_confirm: '' });
  const [pwSaving, setPwSaving] = useState(false);
  const [pwMsg, setPwMsg] = useState('');
  const [pwError, setPwError] = useState('');
  // Section: Profile details — save contact, district and seller identity changes.
  const updateProfile = async (e) => {
    e.preventDefault(); setProfileMsg(''); setProfileError('');
    if (!form.district) { setProfileError('Please select your district.'); return; }
    setProfileSaving(true);
    try {
      await client.patch('/auth/me/', accountPayload(form));
      await refreshUser(); setProfileMsg('Profile updated.');
    } catch (err) { setProfileError(accountError(err)); }
    finally { setProfileSaving(false); }
  };
  // Section: Password change — send the current password and matching new values.
  const changePassword = async (e) => {
    e.preventDefault(); setPwMsg(''); setPwError('');
    if (pwForm.new_password !== pwForm.password_confirm) { setPwError('New passwords do not match.'); return; }
    setPwSaving(true);
    try {
      await client.post('/auth/change-password/', pwForm);
      setPwMsg('Password changed successfully.'); setPwForm({ old_password: '', new_password: '', password_confirm: '' });
    } catch (err) { setPwError(accountError(err)); }
    finally { setPwSaving(false); }
  };
  return <div className="max-w-2xl mx-auto px-6 py-10 profile-settings">
    <h1 className="text-3xl font-bold mb-2">Profile settings</h1>
    <p className="muted mb-6">Manage your {user.role} account, contact details and password.</p>
    <section className="bg-white rounded-2xl border border-ink/10 p-6 mb-6">
      <h2 className="font-bold text-lg mb-4">Profile information</h2>
      <form onSubmit={updateProfile} onInvalidCapture={() => setProfileError('Please complete all fields correctly.')}>
        <fieldset className="account-form" disabled={profileSaving}>
          <AccountFields form={form} setForm={setForm} showHints={false} showIdentityHint={false} />
          {profileMsg && <p className="text-teal" role="status">{profileMsg}</p>}
          {profileError && <p className="form-error" role="alert">{profileError}</p>}
          <button className="tm-button" disabled={profileSaving}>{profileSaving ? 'Saving…' : 'Save changes'}</button>
        </fieldset>
      </form>
    </section>
    <section className="bg-white rounded-2xl border border-ink/10 p-6">
      <h2 className="font-bold text-lg mb-4">Change password</h2>
      <form onSubmit={changePassword} onInvalidCapture={() => setPwError('Fill in all password fields and follow the password rules.')}>
        <fieldset className="account-form" disabled={pwSaving}>
          {[['old_password', 'Current password'], ['new_password', 'New password'], ['password_confirm', 'Confirm new password']].map(([key, label]) => <PasswordField key={key} id={`profile-${key}`} label={label} rules={key === 'new_password'} showHints={false} autoComplete={key === 'old_password' ? 'current-password' : 'new-password'} value={pwForm[key]} onChange={(e) => setPwForm((old) => ({ ...old, [key]: e.target.value }))} />)}
          {pwMsg && <p className="text-teal" role="status">{pwMsg}</p>}
          {pwError && <p className="form-error" role="alert">{pwError}</p>}
          <button className="tm-button" disabled={pwSaving}>{pwSaving ? 'Updating…' : 'Update password'}</button>
        </fieldset>
      </form>
    </section>
  </div>;
}
