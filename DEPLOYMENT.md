# TrustMart hosting

Live website: https://trustmartlk.pythonanywhere.com
API base URL: https://trustmartlk.pythonanywhere.com/api/
Source: https://github.com/jeramkraskens/TrustMart

## Current deployment

PythonAnywhere Beginner (free, no card), Python 3.13, Django WSGI,
React production build, persistent SQLite and signed local uploads.
The website runs on PythonAnywhere even when the developer's PC is off.
The database starts fresh; local users, listings and private files were not uploaded.
Six marketplace categories are initialized. Payments remain the project's demo
escrow simulation; no real payment provider is connected.

The account currently requires renewal before **22 November 2026**. Each month,
open PythonAnywhere > Web and click **Run until 1 month from today**.
The free account has 512 MB storage and limited capacity, suitable for a viva/demo.
Custom domains require a paid PythonAnywhere plan and an owned domain.

Chat and notifications in the hosted web build refresh every 15 seconds.
PythonAnywhere's experimental ASGI proxy reported HTTPS as HTTP, causing redirect
loops. Standard WSGI hosting avoids that limitation and preserves HTTPS redirects.
WebSockets are not enabled on this deployment.

## Paths and settings

- Code: `/home/trustmartlk/TrustMart`
- Virtualenv: `/home/trustmartlk/trustmart-venv`
- WSGI file: `/var/www/trustmartlk_pythonanywhere_com_wsgi.py`
- Django settings: `config.pythonanywhere`
- Frontend build: `deploy/web`
- Database: `backend/db.sqlite3`
- Uploads: `backend/media`
- Secrets: `backend/.env` (server only, excluded from Git)

Force HTTPS is enabled in the PythonAnywhere Web tab. Do not add a public `/media/`
static mapping: uploads use signed, expiring URLs. Django admin is `/django-admin/`;
the React administrator dashboard is `/admin/overview`.

## Create your administrator

In a PythonAnywhere Bash console, run:

```bash
cd ~/TrustMart/backend
DJANGO_SETTINGS_MODULE=config.pythonanywhere ~/trustmart-venv/bin/python manage.py createsuperuser
```

Enter your own username, email and password in the console. Password input is
hidden. No default administrator or public demo password has been created.
Buyers and sellers can register through the website.

## Deploy updates

Build the frontend locally with `VITE_API_URL=/api` and
`VITE_LIVE_TRANSPORT=polling`, copy `frontend/dist` into `deploy/web`, then commit
and push. Never commit `.env`, databases, uploaded evidence or signing keys.
In PythonAnywhere Bash:

```bash
cd ~/TrustMart
 git pull --ff-only origin main
cd backend
export DJANGO_SETTINGS_MODULE=config.pythonanywhere
~/trustmart-venv/bin/python manage.py migrate
~/trustmart-venv/bin/python manage.py collectstatic --noinput
```

Then click **Reload** in the PythonAnywhere Web tab. Back up SQLite and media
before changing schemas; keep backups private and outside the public repository.
If an older browser cached a redirect loop, clear site cache or use a fresh browser.

## Mobile apps

Set the app's server/API URL to:
`https://trustmartlk.pythonanywhere.com/api/`
Existing APKs are not automatically rebuilt by hosting the backend. Their saved
local server address must be changed. WebSocket-only features need HTTP fallback
on this WSGI host; no mobile device integration test has been performed.

## Validation

- Frontend production build passed.
- 21 Django account and hosted-file tests passed on the cloud runtime.
- Public homepage and login render; HTTPS health endpoint returns `{"status":"ok"}`.
- Existing Render configuration is an alternative, not the active deployment.
- The previously created Supabase project is unused by this deployment.

