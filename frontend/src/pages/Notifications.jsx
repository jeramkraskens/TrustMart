import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { notificationLink, useNotifications } from '../context/NotificationContext';

export default function Notifications() {
  const { user } = useAuth();
  const { items, unread, status, error, refresh, markRead, markAllRead } = useNotifications();
  const [visible, setVisible] = useState(24);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState('');
  const alive = useRef(true);
  useEffect(() => {
    alive.current = true;
    return () => { alive.current = false; };
  }, []);
  const act = async (work) => {
    if (busy) return;
    setBusy(true); setActionError('');
    try { await work(); }
    catch { setActionError('Could not update notifications. Please try again.'); }
    finally { if (alive.current) setBusy(false); }
  };
  const rows = items.slice(0, visible);
  return <section className="tm-container notification-page">
    <h1 className="text-3xl font-bold">Notifications</h1>
    <p>{unread} unread · {status}</p>
    <div className="flex gap-3 my-4">
      <button className="tm-button secondary small" disabled={busy} onClick={() => act(refresh)}>Refresh</button>
      <button className="tm-button small" disabled={busy || !unread} onClick={() => act(markAllRead)}>Mark all as read</button>
    </div>
    {(error || actionError) && <p className="form-error" role="alert">{actionError || error}</p>}
    {!rows.length && <p>No notifications yet. New updates will appear here automatically.</p>}
    <ul className="notification-list">{rows.map(item => <li key={item.id} className={item.is_read ? '' : 'unread'}>
      <div><strong>{item.type.replaceAll('_', ' ')}</strong>{!item.is_read && <span className="tm-pill">Unread</span>}</div>
      <p>{item.content}</p><time dateTime={item.created_at}>{new Date(item.created_at).toLocaleString()}</time>
      <div className="flex gap-4 mt-2">
        {item.link && <Link className="text-link" to={notificationLink(item, user.role)}>View update</Link>}
        {!item.is_read && <button className="text-link" disabled={busy} onClick={() => act(() => markRead(item.id))}>Mark as read</button>}
      </div>
    </li>)}</ul>
    {items.length > visible && <button className="tm-button secondary small" onClick={() => setVisible(current => current + 24)}>Load older notifications</button>}
  </section>;
}
