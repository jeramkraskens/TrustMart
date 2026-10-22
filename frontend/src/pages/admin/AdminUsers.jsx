import { useEffect, useState, Fragment } from 'react';
import client from '../../api/client';
import { getAll } from '../../api/marketplace';
import RecordSearch from '../../components/RecordSearch';
import { matchesSearch } from '../../api/search';
import { apiError } from '../../api/errors';
import { useAuth } from '../../context/AuthContext';
import AccountFields from '../../components/AccountFields';
import PasswordField from '../../components/PasswordField';
import { accountPayload } from '../../api/account';

const ROLES = ['buyer', 'seller', 'moderator', 'admin'];

const TABS = [
  { key: '', label: 'All Users' },
  { key: 'buyer', label: 'Buyers' },
  { key: 'seller', label: 'Sellers' },
  { key: 'moderator', label: 'Moderators' },
  { key: 'admin', label: 'Admins' },
];

function AddUserModal({ onClose, onCreated }) {
  const [form, setForm] = useState({
    username: '', email: '', password: '', password_confirm: '', first_name: '', last_name: '', role: 'moderator', phone: '', district: '', nic_number: '', nic_suffix: '',
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const submit = (e) => {
    e.preventDefault();
    if (!form.password_confirm || form.password !== form.password_confirm) {
      setError('Re-enter the same password. Passwords must match.');
      return;
    }
    setSaving(true);
    setError('');
    client.post('/auth/users/', accountPayload(form))
      .then(() => {
        onCreated();
        onClose();
      })
      .catch((err) => {
        const data = err.response?.data;
        setError(data ? Object.values(data).flat().join(' ') : 'Could not create account.');
      })
      .finally(() => setSaving(false));
  };

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div className="bg-[#1a212e] rounded-2xl w-full max-w-md p-6 max-h-[90vh] overflow-y-auto">
        <h2 className="text-lg font-bold mb-1">Add new account</h2>
        <p className="text-sm text-white/40 mb-4">Create a moderator, admin, or any other account directly — no self-registration needed.</p>
        <form onSubmit={submit} className="space-y-3">
          <div>
            <label className="block text-xs font-medium mb-1 text-white/60">Role</label>
            <select
              value={form.role}
              onChange={(e) => setForm({ ...form, role: e.target.value })}
              className="w-full px-3 py-2 rounded-lg border border-white/10 text-sm capitalize"
            >
              {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
            </select>
          </div>
          <AccountFields form={form} setForm={setForm} />
          <div><label htmlFor="admin-username">Username</label>
            <input id="admin-username" className="tm-input" autoComplete="username" value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} required />
          </div>
          <PasswordField id="admin-password" label="Temporary password" rules autoComplete="new-password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
          <PasswordField id="admin-password-confirm" label="Re-enter password" autoComplete="new-password" value={form.password_confirm} onChange={(e) => setForm({ ...form, password_confirm: e.target.value })} />
          {error && <p className="text-xs text-rust break-words" role="alert">{error}</p>}
          <div className="flex gap-2 pt-2">
            <button type="button" onClick={onClose} className="flex-1 py-2.5 rounded-lg border border-white/10 text-sm font-medium">
              Cancel
            </button>
            <button type="submit" disabled={saving} className="flex-1 py-2.5 rounded-lg bg-teal text-white text-sm font-semibold disabled:opacity-50">
              {saving ? 'Creating...' : 'Create account'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
function EditUserModal({ user, onClose, onSaved }) {
  const [form, setForm] = useState({
    username: user.username || '',
    email: user.email || '',
    first_name: user.first_name || '',
    last_name: user.last_name || '',
    phone: user.phone || '',
    role: user.role,
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const submit = (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    client.patch(`/auth/users/${user.id}/`, form)
      .then(() => {
        onSaved();
        onClose();
      })
      .catch((err) => {
        const data = err.response?.data;
        setError(data ? Object.values(data).flat().join(' ') : 'Could not update account.');
      })
      .finally(() => setSaving(false));
  };

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div className="bg-[#1a212e] rounded-2xl w-full max-w-md p-6 max-h-[90vh] overflow-y-auto">
        <h2 className="text-lg font-bold mb-1">Edit account</h2>
        <p className="text-sm text-white/40 mb-4">Update profile details for this {user.role}.</p>
        <form onSubmit={submit} className="space-y-3">
          <div>
            <label className="block text-xs font-medium mb-1 text-white/60">Role</label>
            <select
              value={form.role}
              onChange={(e) => setForm({ ...form, role: e.target.value })}
              className="w-full px-3 py-2 rounded-lg border border-white/10 text-sm capitalize"
            >
              {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
            </select>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium mb-1 text-white/60">First name</label>
              <input value={form.first_name} onChange={(e) => setForm({ ...form, first_name: e.target.value })} required
                className="w-full px-3 py-2 rounded-lg border border-white/10 text-sm" />
            </div>
            <div>
              <label className="block text-xs font-medium mb-1 text-white/60">Last name</label>
              <input value={form.last_name} onChange={(e) => setForm({ ...form, last_name: e.target.value })} required
                className="w-full px-3 py-2 rounded-lg border border-white/10 text-sm" />
            </div>
          </div>
          <div>
            <label className="block text-xs font-medium mb-1 text-white/60">Username</label>
            <input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} required
              className="w-full px-3 py-2 rounded-lg border border-white/10 text-sm" />
          </div>
          <div>
            <label className="block text-xs font-medium mb-1 text-white/60">Email</label>
            <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required
              className="w-full px-3 py-2 rounded-lg border border-white/10 text-sm" />
          </div>
          <div>
            <label className="block text-xs font-medium mb-1 text-white/60">Phone (optional)</label>
            <input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })}
              className="w-full px-3 py-2 rounded-lg border border-white/10 text-sm" />
          </div>
          {error && <p className="text-xs text-rust break-words">{error}</p>}
          <div className="flex gap-2 pt-2">
            <button type="button" onClick={onClose} className="flex-1 py-2.5 rounded-lg border border-white/10 text-sm font-medium">
              Cancel
            </button>
            <button type="submit" disabled={saving} className="flex-1 py-2.5 rounded-lg bg-teal text-white text-sm font-semibold disabled:opacity-50">
              {saving ? 'Saving...' : 'Save changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function AdminUsers() {
  const { user: currentUser } = useAuth();
  const [pendingUser, setPendingUser] = useState(null);
  const [actionError, setActionError] = useState('');
  const [success, setSuccess] = useState('');
  const [users, setUsers] = useState([]);
  const [counts, setCounts] = useState({});
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [activeTab, setActiveTab] = useState('');
  const [expandedId, setExpandedId] = useState(null);
  const [detail, setDetail] = useState(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [loadError, setLoadError] = useState('');
  const scopedUsers = users.filter((u) => !activeTab || u.role === activeTab);
  const userFields = ['username', 'first_name', 'last_name', 'email'];
  const visibleUsers = scopedUsers.filter((u) => matchesSearch(u, userFields, search));

  const load = () => {
    setLoading(true);
    getAll('/auth/users/')
      .then((data) => { setUsers(data); setLoadError(''); })
      .catch(() => setLoadError('Could not load users. Please refresh.'))
      .finally(() => setLoading(false));
  };

  const loadCounts = () => {
    getAll('/auth/users/').then((all) => {
      const c = { '': all.length };
      ROLES.forEach((r) => { c[r] = all.filter((u) => u.role === r).length; });
      setCounts(c);
    }).catch(() => {});
  };

  useEffect(() => {
    const t = setTimeout(load, 300);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(loadCounts, [users.length]);

  const toggleBan = async (u) => {
    setPendingUser(u.id); setActionError(''); setSuccess('');
    try {
      const { data } = await client.patch(`/auth/users/${u.id}/`, { is_banned: !u.is_banned });
      setUsers((old) => old.map((item) => item.id === data.id ? data : item));
      setSuccess(`${data.username} ${data.is_banned ? 'banned. Account access is blocked.' : 'unbanned. They can sign in again.'}`);
    } catch (error) {
      setActionError(apiError(error, 'Could not change the ban status. Please try again.'));
    } finally { setPendingUser(null); }
  };

  const changeRole = (u, role) => {
    client.patch(`/auth/users/${u.id}/`, { role }).then(load);
  };

  const deleteUser = (u) => {
    if (!confirm(`Delete user "${u.username}"? This cannot be undone.`)) return;
    client.delete(`/auth/users/${u.id}/`).then(load);
  };

  const toggleExpand = (u) => {
    if (expandedId === u.id) {
      setExpandedId(null);
      setDetail(null);
      return;
    }
    setExpandedId(u.id);
    setDetail(null);
    Promise.all([
      client.get('/listings/', { params: { all: 'true' } }).then((r) => (r.data.results ?? r.data).filter((l) => l.seller === u.id)),
      client.get('/transactions/').then((r) => (r.data.results ?? r.data).filter((t) => t.buyer === u.id || t.seller_id === u.id)),
    ]).then(([listings, txns]) => {
      setDetail({ listings, txns });
    });
  };

  return (
    <div className="p-6 md:p-8">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Users</h1>
          <p className="text-white/40 text-sm">Manage buyers, sellers and moderators from one place</p>
        </div>
        <button
          onClick={() => setShowAddModal(true)}
          className="px-4 py-2.5 rounded-lg bg-teal text-white text-sm font-semibold hover:bg-teal-dark transition-colors"
        >
          + Add User
        </button>
      </div>

      {showAddModal && (
        <AddUserModal onClose={() => setShowAddModal(false)} onCreated={() => { load(); loadCounts(); }} />
      )}
      {editingUser && (
        <EditUserModal
          user={editingUser}
          onClose={() => setEditingUser(null)}
          onSaved={() => { load(); loadCounts(); }}
        />
      )}

      <div className="flex gap-2 mb-5 border-b border-white/5 overflow-x-auto">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`px-4 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors ${
              activeTab === tab.key ? 'border-teal text-teal' : 'border-transparent text-white/40 hover:text-white'
            }`}
          >
            {tab.label} <span className="text-xs text-white/30">({counts[tab.key] ?? 0})</span>
          </button>
        ))}
      </div>

      <div className="flex gap-3 mb-5">
        <RecordSearch items={scopedUsers} fields={userFields} value={search} onChange={setSearch} label="Search username, email or name" />
      </div>

      {loadError && <p className="form-error" role="alert">{loadError}</p>}
      {actionError && <p className="form-error" role="alert">{actionError}</p>}
      {success && <p className="text-teal mb-4" role="status">{success}</p>}
      <div className="bg-[#1a212e] rounded-xl border border-white/5 overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-white/5 text-left text-white/40 text-xs uppercase tracking-wide">
              <th className="px-4 py-3 font-medium">User</th>
              <th className="px-4 py-3 font-medium">Role</th>
              <th className="px-4 py-3 font-medium">Trust</th>
              <th className="px-4 py-3 font-medium">Activity</th>
              <th className="px-4 py-3 font-medium">Status</th>
              <th className="px-4 py-3 font-medium text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={6} className="px-4 py-8 text-center text-white/30">Loading...</td></tr>
            ) : visibleUsers.length === 0 ? (
              <tr><td colSpan={6} className="px-4 py-8 text-center text-white/30">No users found.</td></tr>
            ) : (
              visibleUsers.map((u) => (
                <Fragment key={u.id}>
                  <tr key={u.id} className="border-b border-white/5 last:border-0 hover:bg-white/5">
                    <td className="px-4 py-3">
                      <button onClick={() => toggleExpand(u)} className="text-left">
                        <p className="font-medium hover:text-teal">{u.username}</p>
                        <p className="text-xs text-white/30">{u.email}</p>
                      </button>
                    </td>
                    <td className="px-4 py-3">
                      <select
                        value={u.role}
                        onChange={(e) => changeRole(u, e.target.value)}
                        className="text-xs px-2 py-1 rounded-md border border-white/10 bg-[#1a212e] capitalize"
                      >
                        {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
                      </select>
                    </td>
                    <td className="px-4 py-3 text-white/60">{u.trust_score}</td>
                    <td className="px-4 py-3 text-xs text-white/40">
                      {u.listings_count} listings · {u.purchases_count} purchases
                    </td>
                    <td className="px-4 py-3">
                      <span className={`text-xs px-2 py-0.5 rounded-full ${u.is_banned ? 'bg-rust-light text-rust' : 'bg-sage-light text-sage'}`}>
                        {u.is_banned ? 'Banned' : 'Active'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right space-x-2">
                      <button
                        onClick={() => setEditingUser(u)}
                        className="text-xs px-3 py-1 rounded-full border border-white/10 text-white/60 hover:border-teal hover:text-teal"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => toggleBan(u)}
                        disabled={pendingUser !== null || u.id === currentUser?.id}
                        title={u.id === currentUser?.id ? 'You cannot ban your own account' : undefined}
                        className={`text-xs px-3 py-1 rounded-full border ${u.is_banned ? 'border-sage text-sage' : 'border-rust text-rust'}`}
                      >
                        {pendingUser === u.id ? 'Saving…' : u.is_banned ? 'Unban' : 'Ban'}
                      </button>
                      <button
                        onClick={() => deleteUser(u)}
                        className="text-xs px-3 py-1 rounded-full border border-white/10 text-white/40 hover:border-rust hover:text-rust"
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                  {expandedId === u.id && (
                    <tr className="bg-white/5">
                      <td colSpan={6} className="px-4 py-4">
                        {!detail ? (
                          <p className="text-xs text-white/30">Loading details...</p>
                        ) : (
                          <div className="grid sm:grid-cols-2 gap-4 text-xs">
                            <div>
                              <p className="font-semibold text-white/55 mb-2 uppercase tracking-wide">Listings ({detail.listings.length})</p>
                              {detail.listings.length === 0 ? (
                                <p className="text-white/30">No listings.</p>
                              ) : (
                                <ul className="space-y-1">
                                  {detail.listings.slice(0, 5).map((l) => (
                                    <li key={l.id} className="flex justify-between">
                                      <span className="truncate">{l.title}</span>
                                      <span className="text-white/30 capitalize ml-2">{l.status}</span>
                                    </li>
                                  ))}
                                </ul>
                              )}
                            </div>
                            <div>
                              <p className="font-semibold text-white/55 mb-2 uppercase tracking-wide">Transactions ({detail.txns.length})</p>
                              {detail.txns.length === 0 ? (
                                <p className="text-white/30">No transactions.</p>
                              ) : (
                                <ul className="space-y-1">
                                  {detail.txns.slice(0, 5).map((t) => (
                                    <li key={t.id} className="flex justify-between">
                                      <span className="truncate">{t.listing_title}</span>
                                      <span className="text-white/30 ml-2">Rs. {Number(t.amount).toLocaleString()}</span>
                                    </li>
                                  ))}
                                </ul>
                              )}
                            </div>
                          </div>
                        )}
                      </td>
                    </tr>
                  )}
                </Fragment>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
