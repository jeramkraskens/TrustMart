import test from 'node:test';
import assert from 'node:assert/strict';
import { createAuthSession, shouldOpenGuestHome } from '../src/api/session.js';

function storage(values = {}) {
  const entries = new Map(Object.entries(values));
  return {
    getItem: (key) => entries.get(key) ?? null,
    setItem: (key, value) => entries.set(key, String(value)),
    removeItem: (key) => entries.delete(key),
  };
}

test('fresh launch removes persistent login for every role, preserving other preferences', () => {
  for (const role of ['buyer', 'seller', 'admin', 'moderator']) {
    const legacyStorage = storage({ access_token: `${role}-access`, refresh_token: `${role}-refresh`, theme: 'light' });
    const session = createAuthSession({ storage: storage(), legacyStorage, runId: 'first-run' });
    assert.equal(session.isFreshSession, true);
    assert.equal(session.getAccessToken(), null);
    assert.equal(session.getRefreshToken(), null);
    assert.equal(legacyStorage.getItem('access_token'), null);
    assert.equal(legacyStorage.getItem('refresh_token'), null);
    assert.equal(legacyStorage.getItem('theme'), 'light');
  }
});

test('refresh keeps an explicit login within the same run, but a project restart clears it', () => {
  const tabStorage = storage();
  const legacyStorage = storage();
  const launch = (runId) => createAuthSession({ storage: tabStorage, legacyStorage, runId });
  const session = launch('run-1');
  session.setTokens({ access: 'signed-in-access', refresh: 'signed-in-refresh' });
  const refreshed = launch('run-1');
  assert.equal(refreshed.isFreshSession, false);
  assert.equal(refreshed.getAccessToken(), 'signed-in-access');
  assert.equal(refreshed.getRefreshToken(), 'signed-in-refresh');
  const restarted = launch('run-2');
  assert.equal(restarted.isFreshSession, true);
  assert.equal(restarted.getAccessToken(), null);
  assert.equal(restarted.getRefreshToken(), null);
});

test('a separate empty tab starts as a guest and logout clears both session tokens', () => {
  const legacyStorage = storage();
  const session = createAuthSession({ storage: storage(), legacyStorage, runId: 'run-1' });
  session.setTokens({ access: 'access', refresh: 'refresh' });
  const newTab = createAuthSession({ storage: storage(), legacyStorage, runId: 'run-1' });
  assert.equal(newTab.getAccessToken(), null);
  session.setTokens({ access: 'renewed-access' });
  assert.equal(session.getRefreshToken(), 'refresh');
  session.clear();
  assert.equal(session.getAccessToken(), null);
  assert.equal(session.getRefreshToken(), null);
});

test('fresh private-page launches return home, while public links and in-session refreshes are preserved', () => {
  for (const path of ['/admin', '/admin/users', '/seller', '/seller/new', '/buyer', '/moderator', '/profile', '/messages', '/wishlist']) {
    assert.equal(shouldOpenGuestHome(path, true), true, path);
    assert.equal(shouldOpenGuestHome(path, false), false, path);
  }
  for (const path of ['/', '/browse', '/categories', '/listings/1', '/login', '/register']) {
    assert.equal(shouldOpenGuestHome(path, true), false, path);
  }
});
