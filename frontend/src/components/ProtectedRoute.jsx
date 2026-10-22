// Screen-level login and role gate.
// The backend must still enforce permissions: hiding a page does not secure an API endpoint.
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// Section: Route access — require login and the requested account role.
export default function ProtectedRoute({ children, roles }) {
  const { user, loading } = useAuth();

  if (loading) {
    return <div className="max-w-6xl mx-auto px-6 py-20 text-center text-ink/50">Loading...</div>;
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (roles && !roles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }

  return children;
}
