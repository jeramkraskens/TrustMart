import { Bell } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useNotifications } from '../context/NotificationContext';

export default function NotificationBell() {
  const { unread } = useNotifications();
  return <Link to="/notifications" className="icon-button notification-bell" aria-label={`Notifications, ${unread} unread`}>
    <Bell size={19} />
    {unread > 0 && <span className="notification-count">{unread > 99 ? '99+' : unread}</span>}
  </Link>;
}
