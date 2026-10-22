# Notifications and alerts

Every signed-in role has a private notification inbox and unread count.
Only the owner can read or mark their notifications. Clients cannot create,
edit or delete notification records through the API.

## Supported events

| Activity | Recipients |
| --- | --- |
| New chat message | Its receiver, with a link to that conversation |
| Verification submission or resubmission | Listing seller, active moderators and admins |
| Approval or rejection | Seller; other admins receive the moderation outcome |
| New order | Buyer, seller and admins |
| Delivery confirmation and demo escrow release | Buyer, seller and admins |
| Dispute opened | Buyer, seller, moderators and admins |
| Dispute resolved | Both participants, the deciding moderator and other admins |
| New review | The reviewed user |
| Account registration or administrator-created account | Account owner and other admins |
| Role change, ban or restored access | Affected account and other admins |
| Password change | Account owner |
| Listing saved as a draft, staff edits or removal | Seller; admins also receive removal notices |

Sensitive messages and order updates are not broadcast to unrelated roles.
Banned and inactive staff accounts do not receive new staff queue notices.
Repeated approval, dispute resolution or delivery confirmation does not create
duplicate decision alerts. Checkout is the existing demonstration flow; these
notices do not represent real payment-provider charges or bank transfers.

## Where to find them

- Website: the bell appears in the marketplace, seller dashboard and admin header.
  `/notifications` supports individual read actions and **Mark all as read**.
- Buyer/seller Android app: the home and toolbar bells show unread counts.
- Moderator Android app: the toolbar bell and Profile → Notifications open the inbox.
- Foreground updates show a website toast or Android snackbar. Choose **View** to
  open the inbox, or **View update** on a record to open its related workspace.

The first history load does not replay old notices as alerts. New events show
alerts while the application is open. Offline notices stay in the database and
appear on the next connection. Read actions update other connected devices.
The clients also reconcile history every 15 seconds to recover missed live events.

## Running

The `notifications.0002_notification_link` migration adds links to saved notices.
It has been applied to the project's configured local database.
For another environment, run `python manage.py migrate` before starting the new code.

Restart the backend and web development server, then rebuild/install both Android
apps. Live updates use the existing JWT authentication and `/ws/notifications/`.
As with chat, production requires a shared `REDIS_URL`, an ASGI server and HTTPS
proxy support for WebSocket upgrades. See [LIVE_CHAT_SETUP.md](LIVE_CHAT_SETUP.md).

Alerts are in-app. Operating-system push notifications when the mobile app is
closed, browser push, SMS and email delivery require a separate push/email service.

## Verification

Run `python manage.py test` for the backend suite. Notification coverage includes
role-specific recipients, private history, read counts, forged-write rejection,
transaction rollback, private live delivery and read synchronization.
