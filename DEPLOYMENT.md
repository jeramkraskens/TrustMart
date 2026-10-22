# TrustMart free demo deployment

Status: deployment configuration prepared. Supabase project `nfdnujrtikfhrokahnyk`
exists in Singapore. GitHub repository: https://github.com/jeramkraskens/TrustMart
(public, currently empty). GitHub source upload was rejected with HTTP 403
"Resource not accessible by integration"; repository write access must be fixed.
No working public application deployment has been verified.

Connect GitHub, Render, and Supabase to complete deployment. Keep the source
repository private. Upload source only, respecting .gitignore; do not upload
backend/.env, databases, media, virtual environments, or Android signing keys.

## Services

- Render: static React site, free Django ASGI service, free Key Value instance.
- Supabase Free: PostgreSQL and a PRIVATE storage bucket named trustmart-media.
- Android: existing APKs use the hosted HTTPS API in their server settings.

## Configuration

1. Create a free Supabase project. Copy its PostgreSQL session-pooler connection
   string into Render's DATABASE_URL secret (URL-encode the password).
2. Create the private trustmart-media bucket. In Supabase Storage S3 settings,
   generate server-side S3 keys. Put the access key, secret, endpoint, and region
   in the corresponding Render S3_* environment variables. Do not put these in
   the React build or Android apps.
3. Push this folder as the GitHub repository root, then create a Render Blueprint
   using render.yaml. Confirm all compute plans remain free.
4. Set VITE_API_URL to the actual backend HTTPS URL followed by /api.
   Set CORS_ALLOWED_ORIGINS and CSRF_TRUSTED_ORIGINS to the actual frontend HTTPS
   origin (no trailing slash). Redeploy affected services after setting URLs.
5. The backend runs collectstatic during build and migrations at startup. The
   production settings require DEBUG=False, Redis, a strong secret, PostgreSQL,
   and private S3 storage. Local development still uses config.settings.
6. Create privileged accounts through a secure management session; do not seed
   the publicly documented demo administrator passwords on the public service.
7. In both Android apps, set the server URL to https://YOUR-API-HOST/api/.

## Verification before declaring the site live

- Resolve dependency installation on the hosting Python runtime; existing pinned
  dependencies have not yet been verified against the remote package index.
- GET /api/health/ should return {"status":"ok"} over HTTPS.
- Verify frontend refresh on a nested route, registration/login, token refresh,
  listing browsing, authorized upload/download, moderation, and mobile login.
- Restart the backend and confirm database records and uploads survive.
- Confirm private evidence files are accessible only through authorized API
  responses. Signed URLs expire after one hour; reload to obtain fresh URLs.
- Test recommendation memory usage on the free backend before the viva.

Render's free backend sleeps after inactivity and has ephemeral local storage.
Supabase Free projects may pause after inactivity. No local database or uploaded
files have been migrated by these configuration changes. Keep a local demo backup.

References: https://render.com/docs/blueprint-spec,
https://render.com/docs/free,
https://supabase.com/docs/guides/storage/s3/authentication
