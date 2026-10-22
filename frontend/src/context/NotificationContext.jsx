import { createContext, useContext, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from './AuthContext';
import client from '../api/client';
import { getAll } from '../api/marketplace';
import { connectLiveUpdates } from '../api/liveMessages';

const empty = { items: [], unread: 0, status: '', error: '', refresh: async () => {}, markRead: async () => {}, markAllRead: async () => {} };
const Context = createContext(empty);
export const useNotifications = () => useContext(Context);

export function notificationLink(item, role) {
  let link = item.link || '/notifications';
  if (!link.startsWith('/') || link.startsWith('//')) return '/notifications';
  if (role === 'admin' && link === '/moderator') link = '/admin/verifications';
  if (role === 'admin' && link.startsWith('/moderator/')) link = link.replace('/moderator/', '/admin/');
  return link;
}

export function NotificationProvider({ children }) {
  const { user } = useAuth();
  return user ? <NotificationSession key={user.id} user={user}>{children}</NotificationSession>
    : <Context.Provider value={empty}>{children}</Context.Provider>;
}

function NotificationSession({ user, children }) {
  const [items, setItems] = useState([]);
  const [unread, setUnread] = useState(0);
  const [status, setStatus] = useState('Connecting…');
  const [error, setError] = useState('');
  const [alert, setAlert] = useState(null);
  const latest = useRef(null);
  const refreshRef = useRef(async () => {});

  useEffect(() => {
    let stopped = false;
    let running = false;
    let again = false;
    const refresh = async () => {
      if (running) { again = true; return; }
      running = true;
      do {
        again = false;
        try {
          const history = await getAll('/notifications/');
          if (stopped) break;
          const rows = [...new Map(history.map(item => [item.id, item])).values()].sort((a, b) => b.id - a.id);
          const highest = rows.reduce((value, item) => Math.max(value, item.id), latest.current ?? 0);
          if (latest.current !== null) {
            const added = rows.filter(item => item.id > latest.current && !item.is_read);
            if (added.length) setAlert({ ...added[0], extra: added.length - 1 });
          }
          latest.current = highest;
          setItems(rows);
          setUnread(rows.filter(item => !item.is_read).length);
          setError('');
        } catch {
          if (!stopped) setError('Could not update notifications. Retrying automatically.');
        }
      } while (again && !stopped);
      running = false;
    };
    refreshRef.current = refresh;
    refresh();
    const disconnect = connectLiveUpdates('notifications', refresh, setStatus);
    return () => { stopped = true; disconnect(); };
  }, [user.id]);

  useEffect(() => {
    if (!alert) return;
    const timer = setTimeout(() => setAlert(null), 8000);
    return () => clearTimeout(timer);
  }, [alert]);

  const markRead = async (id) => {
    await client.post(`/notifications/${id}/mark_read/`);
    setItems(current => current.map(item => item.id === id ? { ...item, is_read: true } : item));
    await refreshRef.current();
  };
  const markAllRead = async () => {
    await client.post('/notifications/mark_all_read/');
    setItems(current => current.map(item => ({ ...item, is_read: true })));
    await refreshRef.current();
  };
  return <Context.Provider value={{ items, unread, status, error, refresh: () => refreshRef.current(), markRead, markAllRead }}>
    {children}
    {alert && <aside className="notification-toast" role="status" aria-live="polite">
      <strong>New notification</strong>
      <p>{alert.content}</p>
      {alert.extra > 0 && <small>And {alert.extra} more updates</small>}
      <div><Link to={notificationLink(alert, user.role)} onClick={() => setAlert(null)}>View update</Link>
        <button type="button" onClick={() => setAlert(null)} aria-label="Dismiss notification">Dismiss</button></div>
    </aside>}
  </Context.Provider>;
}
