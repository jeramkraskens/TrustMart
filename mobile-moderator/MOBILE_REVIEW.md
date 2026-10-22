# Mobile review against the seven supplied UI references

The references are screenshots, not an editable Figma file. Colors and layouts
were aligned by inspection; pixel-level device verification is still required.
Example names, counts, ratings, urgency labels and SLA times are not live data.

## Implemented

- Shared navy/teal/pale-gray palette, colored dashboard cards, corrected card
  spacing, and system-bar/keyboard insets.
- Scrollable login form, empty-input validation and password recovery guidance.
  Username login is retained because the existing API authenticates usernames.
- All-page loading for reviews, disputes and messages. Counts no longer silently
  stop at the backend's first 12 results.
- All/Pending/Done review filters and All/Pending/Resolved dispute filters.
- Refresh on returning to home, profile and queues; explicit retry controls.
- Read-only nested listing details in verification/dispute API responses; actual
  photos, price, category and condition on review screens. Photos have loading,
  missing and failure states. Additional photos scroll horizontally.
- Approval requires all four review checks; rejection requires written feedback.
  Reviewed items cannot be actioned again from the mobile screen. Inputs survive
  rotation. These are UI safeguards, not server-enforced workflow guarantees.
- Dispute decisions require notes and confirmation; resolved cases are read-only
  for decision buttons. Product photos are not presented as dispute evidence.
- Personal profile statistics filter by moderator ID. Edit name and change
  password use existing API endpoints. History rows navigate to queues (select
  Done or Resolved); report dialog summarizes personal totals.
- Conversation opening and replies, error display, explicit refresh and draft
  retention on failed sends. Starting a conversation with a new user is not added.
- JWT refresh preserves rotated refresh tokens; unsuccessful refresh no longer
  returns a prematurely closed HTTP response. Login avoids stale bearer headers.
- Responsibility comments added to every mobile Kotlin source file, plus comments
  for key lifecycle, pagination, review and authentication decisions.

## Remaining work to fully deliver the design

| Feature | Missing implementation |
| --- | --- |
| Urgent/Standard filters, High/Medium/Low priority, SLA timer | Define and store priority and deadline rules on the backend, then expose them in the API. |
| NIC verification and market reference price | Add an authorized verification workflow and trustworthy price data; do not invent verified identity status or expose full identity numbers. |
| Fraud alerts and user reports | Report model, review endpoints and corresponding moderation screens. Dashboard currently shows real reviews/disputes only. |
| Dispute evidence gallery | Attachment model, authenticated upload/read permissions and evidence UI. Current API only stores a reason. |
| Escalate to Admin | Escalation state, endpoint, admin routing and audit trail. Current button explains that it is unavailable. |
| Notifications | Mobile inbox and read state integration. Profile currently explains availability. |
| Dark mode and languages | Theme persistence, replacement of fixed white surfaces, translated resources and device checks. Current theme is English/light. |
| Email sign-in and email recovery | Backend authentication/recovery flow. Username sign-in and administrator recovery guidance remain. |
| Production chat | New-conversation selection, read receipts and live updates; current conversations refresh manually. |
| Session expiry | Global redirect to login after an unrecoverable refresh failure. Current screens expose request failures. |

## Backend workflow issues found during review

These pre-existing issues need a separate, tested workflow pass before real money
or untrusted users are involved:

- Dispute resolution saves its resolved state before the payment-provider call.
  A payment failure can leave the dispute and transaction inconsistent.
- Review and resolution endpoints need server-side state checks, ownership checks
  on creation, concurrency protection, and idempotency for repeated decisions.
- User/model viewsets expose more generic mutations than the moderation UI uses;
  object permissions need a dedicated review.

## Build and run

Use the installed JDK 21 in Android Studio's Gradle JDK setting. Gradle 8.7 in this
project cannot run with the installed default JDK 25. The Gradle distribution is
cached locally, but the repository is missing gradle/wrapper/gradle-wrapper.jar;
the checked-in gradlew scripts alone do not work on a fresh machine. Restore the
official wrapper with the matching Gradle distribution before distributing it.

Restart Django so the new read-only listing fields are available. No database
migration is needed. Keep the existing phone API address aligned with your
computer's current LAN address and start Django on 0.0.0.0:8000.

## Physical-phone acceptance checks

Automated verification: debug APK assembly and Android lint completed successfully
using JDK 21. Django system checks passed, and both new mobile metadata contract
tests passed against a temporary SQLite test database. No physical-phone visual
or end-to-end payment testing was performed in this pass.

1. Login with moderator1; check wrong-password and disconnected-server feedback.
2. Open keyboard on a short display; scroll to Sign In without clipped controls.
3. Create more than 12 test reviews/disputes and verify counts and filters.
4. Open a review with multiple photos and long title/seller names. Check photo
   failures are visible and the content remains scrollable.
5. Complete the checklist, approve, and return to the queue/home. Verify refresh.
6. Reject another pending item with feedback. Rotate before submitting and verify
   entered feedback/checklist remain. Reviewed items must not allow repeat taps.
7. Open a dispute, enter a ruling note, cancel confirmation, then confirm using
   test data only. Check resolved status after returning.
8. Edit profile name, change password, and verify personal counts exclude other
   moderators. Use queue filters for history.
9. Open an existing chat, reply, refresh and check the reply appears once.
10. Verify bottom navigation, back navigation, large font settings and status/nav
    bars on the physical phone. Log out and confirm the login screen appears.
