# Live conversations

Buyer–seller and moderator–seller chats use the same private message API.
The website, TrustMart Android app and moderator Android app subscribe to
`/ws/messages/` while their inbox or conversation is open. Messages are saved
with `POST /api/messages/`; only the sender and recipient get a change event.
Clients reload their authorized history on connection and after each event.
They also recover missed events every 15 seconds and retry broken connections.
Android connections stop when the chat screen is no longer started.

## Local development

From `backend`, install requirements in the existing virtual environment, then:

```powershell
.\.venv\Scripts\python.exe manage.py runserver 0.0.0.0:8000
```

Restart an already running backend after installing these dependencies.
Daphne now provides the ASGI development server, including WebSockets.
With `DEBUG=True` and no `REDIS_URL`, one server process uses an in-memory
channel layer. No database migration is needed for live messaging.

Use the same backend address in the web and Android apps. A physical phone
needs the computer's LAN address, not localhost. Set `ALLOWED_HOSTS` for that
address and `CORS_ALLOWED_ORIGINS` to the exact website origins used by browsers.
The clients derive the WebSocket URL from their existing API base URL.

## Production

Set `REDIS_URL` (for example `redis://127.0.0.1:6379/0`) and `DEBUG=False`.
All backend processes must use the same Redis service. Run an ASGI server:

```text
daphne -b 0.0.0.0 -p 8000 config.asgi:application
```

Configure the HTTPS reverse proxy to forward WebSocket upgrades for
`/ws/messages/` to this server as well as HTTP requests. HTTPS clients use WSS.
JWT credentials are sent in the first WebSocket frame, never in query strings.
Connections expire with the access token; the clients use the normal HTTP
refresh flow before reconnecting. Blocked accounts cannot subscribe or receive
further change events.

## Try it

1. Sign in as a buyer and seller in separate browser sessions or devices.
2. Send a message from a listing, then open Messages on both sides.
3. Reply in either direction; the other conversation updates automatically.
4. As a moderator, use **Chat with seller** from a verification review.
5. Confirm the buyer cannot see this moderator–seller conversation.
6. Disconnect one device, send a message, then reconnect: history catches up.

WebSocket updates apply while chat is open. Closed-app push notifications,
typing indicators and read receipts are separate features.

Backend verification: `python manage.py test messaging`.
