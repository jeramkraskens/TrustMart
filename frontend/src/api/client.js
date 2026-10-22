// Shared HTTP connection from the website to Django.
// All callers use the same server address, access-token attachment and expired-token retry handling.
import axios from "axios";
import { createAuthSession } from './session';

export const authSession = createAuthSession({
  storage: window.sessionStorage,
  legacyStorage: window.localStorage,
  runId: import.meta.env.VITE_APP_RUN_ID,
});

const API_BASE = import.meta.env.VITE_API_URL || "http://127.0.0.1:8000/api";

const client = axios.create({ baseURL: API_BASE });

// Section: Authenticated requests — attach the current access token.
client.interceptors.request.use((config) => {
  const token = authSession.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let isRefreshing = false;
let queue = [];

// Section: Token refresh — queue failed requests while renewing access.
client.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    if (
      error.response?.status === 401 &&
      !original._retry &&
      !original.url?.includes("/auth/login/")
    ) {
      // Allow only one retry for this request, avoiding an endless loop if renewed credentials also fail.
      original._retry = true;
      const refresh = authSession.getRefreshToken();
      if (!refresh) {
        authSession.clear();
        window.location.href = "/login";
        return Promise.reject(error);
      }
      // Wait for the refresh already in progress instead of sending several refresh requests at once.
      if (isRefreshing) {
        return new Promise((resolve, reject) =>
          queue.push({ resolve: () => resolve(client(original)), reject }),
        );
      }
      isRefreshing = true;
      try {
        const { data } = await axios.post(`${API_BASE}/auth/login/refresh/`, {
          refresh,
        });
        authSession.setTokens(data);
        queue.forEach((pending) => pending.resolve());
        queue = [];
        return client(original);
      } catch (e) {
        queue.forEach((pending) => pending.reject(e));
        queue = [];
        authSession.clear();
        window.location.href = "/login";
        return Promise.reject(e);
      } finally {
        isRefreshing = false;
      }
    }
    return Promise.reject(error);
  },
);

export const mediaUrl = (path) => {
  if (!path) return null;
  if (path.startsWith("http")) return path;
  return `${API_BASE.replace("/api", "")}${path}`;
};

export default client;
