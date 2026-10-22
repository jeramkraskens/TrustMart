# TrustMart App

> **30 September integration:** This app now targets the final website's registration,
> location/contact, listing-management and simulated-checkout APIs. Both Card and
> PayHere are demos. See [current integration and run guide](../FINAL_PROJECT_INTEGRATION.md).

Separate **native Android/Kotlin + Jetpack Compose** buyer-and-seller application.
Application ID: `com.trustmart.app`. It installs alongside `com.marketplace.moderator`.
The existing Django backend is shared with the React website; there is no second database.
This directory can be opened independently in Android Studio. No website WebView is used.

The buyer/seller UI now follows the 15 uploaded design screenshots. See
[UI_REDESIGN.md](UI_REDESIGN.md) for the screen map and intentional backend-related differences.
See [BUG_FIXES.md](BUG_FIXES.md) for the client bug audit and remaining release work.

## Open and run on your physical Android phone

1. Android Studio > **Open** > `C:\Projects\Second Hand Market\TrustMart App` (not `mobile-moderator`).
2. Use Gradle JDK **21** (the installed `C:\Users\Avishka Harshan\.jdks\jbr-21.0.11` works), and let Gradle sync.
   The wrapper pins Gradle 8.7 and the project uses the existing Android SDK 34.
3. Start PostgreSQL and the existing backend in PowerShell:

   ```powershell
   cd 'C:\Projects\Second Hand Market\backend'
   .\.venv\Scripts\python.exe manage.py runserver 0.0.0.0:8000
   ```

4. Connect your phone via USB, enable developer options and USB debugging, approve your computer,
   select your device in Android Studio, and press Run. Alternatively install the debug APK from
   `app\build\outputs\apk\debug\app-debug.apk` after building.
5. For USB: run `adb reverse tcp:8000 tcp:8000`, then save `http://127.0.0.1:8000/api/`
   in the app's **gear** settings. Keep USB connected and repeat forwarding after reconnecting.
   For Wi-Fi: keep phone and PC on the same private Wi-Fi, run `ipconfig` and find the PC's Wi-Fi IPv4 address.
6. In **TrustMart App**, tap the top-right **gear** and save `http://YOUR-PC-IP:8000/api/`.
   For Wi-Fi, do NOT use `localhost` on a phone. Emulator default: `http://10.0.2.2:8000/api/`.
   Saving a different server signs you out. Set this only to a server you trust.
7. Ensure that PC IP is in Django's `ALLOWED_HOSTS` in `backend/.env`, then restart Django.
   Allow TCP port 8000 through Windows Firewall on your **private network only**, if needed.
8. Sign in with the same **username** and password you use on the website, or register a buyer/seller.
   Admin and moderator accounts are intentionally not supported here.

The frontend does **not** need to run for this Android app. The backend and PostgreSQL do.
No credentials or Stripe secret keys belong in the Android project.

## Implemented feature map

| Website function | Native destination | Existing API |
| --- | --- | --- |
| Browse, search, category/condition filter, price sort | Home / Search | `listings/`, `categories/` |
| Personalized recommendations | Home | `recommendations/`, search/view logging |
| Listing photos, seller details, reviews | Product details | `listings/{id}/`, `reviews/` |
| Buyer/seller registration and login | Account | `auth/register/`, `auth/login/`, `auth/me/` |
| Wishlist toggle and saved listings | Heart button | `wishlist/` |
| Seller listings and moderation feedback | Listings | `listings/?mine=true`, `verifications/` |
| Create listing, multiple photos, receipt/PDF evidence | Home > Add New Listing | multipart `listings/`, `verifications/` |
| Retry a draft's verification submission | Listings / Add New Listing | `verifications/` |
| Card simulation (debug only) | Product > Buy | demo `transactions/` |
| PayHere simulation | Checkout (debug only) | demo `transactions/` |
| Purchases and seller sales | Orders | `transactions/` |
| Confirm delivery after inspection | Orders > Track Order | `confirm_delivery/` |
| Raise/view disputes and moderator feedback | Orders | `disputes/` |
| Review a completed purchase | Orders | `reviews/` |
| Start buyer/seller messages and reply | Product/Order > Message; Chat | `messages/` |
| Notification list and mark-read | Bell button | `notifications/` |
| Edit profile, change password, logout | Account | `auth/me/`, `auth/change-password/` |

Lists consume all API pages rather than silently stopping at Django's 12-record first page.
Chat polls every 15 seconds **only while the screen is started** and also supports manual refresh.
There are no fabricated typing, delivered or read receipts; the backend does not implement those.
New conversations start from product details or an order. A contact-moderator assignment workflow
is not present on the existing website/backend and is not invented here.

## Payment safety and current backend limits

This is a development/demo client, **not a production marketplace release**.

- Both Card and PayHere are **simulated**, just like the website. Checkout is available only in debug builds, explicitly
  confirms that no money is collected, and still creates an order/marks the listing sold.
- No payment, delivery confirmation, dispute, message or listing was created by the automated smoke check.
  User interactions with those buttons DO change your shared database.
- No card details or payment SDK are needed. A failed purchase response can still be ambiguous;
  check Orders before attempting another purchase.
- Legacy Stripe recovery stores the buyer/listing/intent ID encrypted. It can find an existing order,
  but cannot create a new payment or simulated order. Ask an administrator to reconcile an unmatched authorization.
- Resetting recovery does not cancel a Stripe hold. If a test authorization succeeded but no order was
  recorded, keep recovery information and reconcile with the backend/Stripe before resetting.

Before production, review server-side authorization, delivery/dispute state transitions,
checkout idempotency, real payment reconciliation and seller payouts. The demo backend already
locks inventory during purchase creation, but client checks and read-only contract tests do not
establish production readiness. This Android audit did not change backend behavior.

## Structure

- `app/src/main/java/com/trustmart/app/MainActivity.kt`: navigation and role gates.
- `AppViewModel.kt`: action orchestration, retry state, error handling and API-backed screen state.
- `data/Models.kt`: typed Django response models and eligibility helpers.
- `data/Api.kt`: transport, paging, JWT refresh, multipart document uploads, server validation.
- `data/SessionInterceptor.kt`: authenticated requests and safe token-refresh failures.
- `data/SessionStore.kt`: Android Keystore AES-GCM encryption for JWTs and checkout recovery.
- `ui/`: native screen groups and reusable accessible form components.
- `app/src/test/.../ContractTest.kt`: local JVM contract and action-eligibility checks.
- `tools/check_backend_contract.py`: read-only checks against your configured Django database.

Code comments explain the important state, security and retry decisions. Android backups are disabled.
Release builds disallow cleartext HTTP; debug builds allow your local development backend.
Uploaded files are limited to 10 MB each, with up to 6 photos; only picker-selected files are accessed.
Large marketplace datasets should migrate from all-page collection to server-backed incremental paging.
For a large chat deployment, use conversation endpoints and incremental message sync rather than fetching
the complete history. Push notifications/offline sending are not implemented.

## Build and checks

```powershell
cd 'C:\Projects\Second Hand Market\TrustMart App'
$env:JAVA_HOME = 'C:\Users\Avishka Harshan\.jdks\jbr-21.0.11'
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug --no-daemon
```

If Java reports `Unable to establish loopback connection` on Windows, use a short existing
temporary path for this shell, then rerun Gradle:

```powershell
$buildTemp = (New-Item -ItemType Directory -Force 'app/build/tmp/java').FullName
$env:TEMP = $buildTemp
$env:TMP = $buildTemp
$env:JAVA_TOOL_OPTIONS = '-Djava.net.preferIPv4Stack=true -Djava.io.tmpdir="' + $buildTemp + '"'
```

Read-only backend check (needs the existing database, but no running HTTP server):

```powershell
cd 'C:\Projects\Second Hand Market'
.\backend\.venv\Scripts\python.exe 'TrustMart App\tools\check_backend_contract.py'
```

Device layout tests (installs the debug app/test APK on a connected test device):

```powershell
cd 'C:\Projects\Second Hand Market\TrustMart App'
.\gradlew.bat connectedDebugAndroidTest
```

These use isolated fixture preferences, not your account or live marketplace writes.
Use tools/pull-ui-screenshots.ps1 with the device ID to transfer generated PNGs into
app/build/reports/ui-screenshots. They are device renders, not end-to-end payment or upload tests.

## Physical-device acceptance checklist

Build/static checks cannot prove real device, file provider, network or payment behavior. Before a demo:

1. Connect using the PC IP; register/sign in as each role; restart the app and check the session.
2. Search/filter; tap product photo thumbnails; save/unsave wishlist; test empty states and disconnected Wi-Fi.
3. Seller: select three photos and optional PDF evidence; submit once; verify it appears in the moderator queue.
4. Approve the listing in the separate moderator app; check it is visible to buyers.
5. Buyer: message the seller; seller replies; wait 15 seconds or Refresh to see the response.
6. Use disposable test listings for demo checkout. Cancel once, confirm once, and check exactly one order.
7. Check Orders after losing the network during checkout before retrying.
8. On separate test orders, raise a dispute OR confirm delivery, then review a released order.
9. Change profile/password; read notifications; sign out and confirm no other account's data remains.
10. Rotate the phone while typing or uploading; verify drafts, keyboard layout, large text and Android Back.

## Compose compiler configuration

Kotlin and `org.jetbrains.kotlin.plugin.compose` both use **2.0.21**. Keep these
plugin versions identical in `build.gradle.kts`; the app module applies both.
The Compose plugin replaces the older `composeOptions.kotlinCompilerExtensionVersion`
setting. Open `TrustMart App` as its own Android Studio project, then use
**File > Sync Project with Gradle Files** after changes to this configuration.
A stale or missing Compose plugin import can produce many
`Composable function call without Compose plugin` errors across screen files.

If Gradle builds successfully but Studio still reports unresolved declarations
within their own file (for example `CategoryDesign` in `CategoryIcons.kt`), use
**File > Repair IDE** and then **Rescan Project Indexes**. Allow project analysis
to finish before assessing the Problems panel. Category icons are used by the
marketplace screens; do not remove them based on stale unused-code diagnostics.

The app is intended for local testing. Signing, Play Store requirements, a production API domain,
payment compliance and a production backend security review are separate release work.
