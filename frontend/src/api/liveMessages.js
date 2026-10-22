import client, { authSession } from './client';

// Credentials travel in the first frame, never in URLs or proxy access logs.
export function connectMessages(onChange, onStatus) {
  return connectLiveUpdates('messages', onChange, onStatus);
}

export function connectLiveUpdates(resource, onChange, onStatus = () => {}) {
  // Standard WSGI hosting uses the same authenticated HTTP endpoints.
  if (import.meta.env.VITE_LIVE_TRANSPORT === 'polling') {
    onStatus('Auto-refresh');
    onChange();
    const poll = setInterval(() => {
      if (document.visibilityState === 'visible') onChange();
    }, 15000);
    return () => clearInterval(poll);
  }
  let stopped = false;
  let socket;
  let retry;
  let handshake;
  let failures = 0;
  const connect = async () => {
    try {
      // This also refreshes expired credentials through the existing interceptor.
      await client.get('/auth/me/');
      if (stopped) return;
      const url = new URL(client.defaults.baseURL, window.location.origin);
      url.pathname = url.pathname.replace(/\/api\/?$/, `/ws/${resource}/`);
      url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
      socket = new WebSocket(url);
      handshake = setTimeout(() => socket?.close(), 10000);
      socket.onopen = () => socket.send(JSON.stringify({ token: authSession.getAccessToken() }));
      socket.onmessage = (event) => {
        if (stopped) return;
        const data = JSON.parse(event.data);
        if (data.type === 'ready') {
          clearTimeout(handshake);
          failures = 0;
          onStatus('Live');
          onChange();
        } else if (data.type === `${resource}.changed`) onChange();
      };
      socket.onclose = schedule;
      socket.onerror = () => socket.close();
    } catch {
      if (!stopped) schedule();
    }
  };
  const schedule = () => {
    clearTimeout(handshake);
    if (stopped) return;
    onStatus('Reconnecting…');
    clearTimeout(retry);
    retry = setTimeout(connect, Math.min(30000, 1000 * 2 ** Math.min(failures++, 5)));
  };
  onStatus('Connecting…');
  connect();
  // Recovery if a broadcast is lost, even while the socket appears connected.
  const fallback = setInterval(() => {
    if (document.visibilityState === 'visible') onChange();
  }, 15000);
  return () => {
    stopped = true;
    clearTimeout(retry);
    clearInterval(fallback);
    clearTimeout(handshake);
    socket?.close();
  };
}
