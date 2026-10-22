// Shared login state for the React component tree.
// Pages use useAuth() to read the current user or call login, registration and logout actions.
import { createContext, useContext, useEffect, useState } from 'react';
import client, { authSession } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Section: Restore the signed-in user for the current project run.
  const loadMe = async () => {
    const token = authSession.getAccessToken();
    if (!token) {
      setLoading(false);
      return;
    }
    try {
      const { data } = await client.get('/auth/me/');
      setUser(data);
    } catch {
      authSession.clear();
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadMe();
  }, []);

  // Section: Login — save session tokens and expose account details.
  const login = async (username, password) => {
    const { data } = await client.post('/auth/login/', { username, password });
    authSession.setTokens(data);
    setUser(data.user);
    return data.user;
  };

  // Section: Registration — create the account, then sign in.
  const register = async (payload) => {
    await client.post('/auth/register/', payload);
    return login(payload instanceof FormData ? payload.get('username') : payload.username, payload instanceof FormData ? payload.get('password') : payload.password);
  };

  const logout = () => {
    authSession.clear();
    setUser(null);
  };

  const refreshUser = async () => {
    const { data } = await client.get('/auth/me/');
    setUser(data);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, refreshUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
