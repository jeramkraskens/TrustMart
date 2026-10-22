// Stores login tokens for the current browser session and project run.
// The access token accompanies API calls; the refresh token requests a replacement when access expires.
const RUN_KEY = 'trustmart:auth:run';
const TOKEN_KEYS = ['access_token', 'refresh_token'];

// Section: Guest startup — discard tokens from earlier project runs.
export function createAuthSession({ storage, legacyStorage, runId }) {
  // Remove old persistent logins without deleting unrelated browser preferences.
  TOKEN_KEYS.forEach((key) => legacyStorage.removeItem(key));
  const isFreshSession = storage.getItem(RUN_KEY) !== runId;
  const clear = () => TOKEN_KEYS.forEach((key) => storage.removeItem(key));
  if (isFreshSession) {
    clear();
    storage.setItem(RUN_KEY, runId);
  }
  return {
    isFreshSession,
    getAccessToken: () => storage.getItem('access_token'),
    getRefreshToken: () => storage.getItem('refresh_token'),
    setTokens: ({ access, refresh }) => {
      storage.setItem('access_token', access);
      if (refresh) storage.setItem('refresh_token', refresh);
    },
    clear,
  };
}

// Section: Startup redirect — return restored private pages to the homepage.
export function shouldOpenGuestHome(pathname, isFreshSession) {
  return isFreshSession && /^\/(admin|seller|buyer|moderator|profile|messages|wishlist)(\/|$)/.test(pathname);
}
