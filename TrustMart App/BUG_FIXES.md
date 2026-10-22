# TrustMart client bug audit — 26 September 2026

## Follow-up — 12 October 2026

- Session refresh now reports malformed responses as recoverable network errors instead
  of allowing runtime exceptions to escape OkHttp's asynchronous worker. It validates
  both returned tokens before writing either, preserves credentials on temporary errors,
  and closes the original unauthorized response if refresh fails.
- Product detail clears the previous seller's reviews before loading another product.
- The listing editor retains selected photos when the picker is cancelled and requests
  persistent read permission for accepted documents so restored drafts can still read them.
- Acknowledged wishlist changes and mark-read actions remain successful when a subsequent
  refresh fails. The product wishlist icon reflects the saved state.
- Removed the unused Stripe SDK, payment callback and response model from simulated checkout.
  Existing legacy recovery records remain supported for reconciliation.
- Replaced boxed integer Compose states flagged by lint, corrected stale checkout/setup
  instructions, and documented the Windows Java loopback workaround.

Regression coverage adds six JVM session/transport tests and a device test for the product
wishlist state. Tests use mock HTTP responses and isolated screen fixtures; the backend
contract check only reads existing data. Dependency-version advisories remain visible:
upgrading the Android/Kotlin/Compose toolchain is separate from these behavioral fixes.

Verification on 12 October:

- Debug APK built successfully; all 36 JVM tests passed.
- Lint completed with zero errors and nine dependency-version advisories. No lint
  warnings were suppressed; the two Compose boxing findings are resolved.
- All 22 authenticated read-only buyer/seller API contract checks passed.
- Installed the updated APK on Samsung SM-A525F without clearing app data.
- Device suite: the first run passed 26/27; registration screenshot capture had a
  transient missing-Compose-root failure. That test passed alone, then the complete
  27-test suite passed in 43.253 seconds. The initial failure is retained in
  `app/build/reports/device-audit-20261012.txt`; the passing rerun is in
  `app/build/reports/device-audit-recheck-20261012.txt`.
- These fixture tests do not exercise real photo providers or a complete live-account
  purchase/upload flow. No live listings, orders, messages or reviews were created.

## Follow-up: product screen force-close

The phone crash buffer recorded repeated `IllegalStateException` failures, including
26 September at 15:26:08, for `com.trustmart.app`: a vertically scrolling component
was measured with infinite height. `ListingScreen` placed the scrolling `Page`
component inside its existing scrolling column when no matching product was loaded.
This branch runs before a request starts, after a failed request, or when opening
a different product. The original screen tests always supplied a matching product
and missed this branch.

The fallback now uses a non-scrolling padded column inside the existing scroller,
with explicit loading text and a retry action on failure. Three regression tests cover
missing product data, loading-to-failure-to-success transitions, and a different
product ID with stale cached detail. Device execution status is in VERIFICATION.md.

## Confirmed problems addressed

1. **Sent message looked unsent after a refresh error.** The POST could succeed, but the
   following GET could fail before the draft was cleared. The response is now retained locally
   and acknowledgement clears only the submitted draft before refresh. A newer draft is preserved.
2. **Successful submissions reported as generic failures.** Listing submission, disputes,
   reviews and purchases now distinguish acknowledged writes from failed subsequent refreshes.
   The user is told to refresh, not to repeat an already successful submission.
3. **Stale dispute/review actions.** Both re-fetch order state and check the current buyer,
   order eligibility and existing records before POST. The created record is retained locally.
   These client guards supplement, but do not replace, backend authorization and idempotency.
4. **Conversation filtering assumed every returned message belonged to the current user.**
   Inbox grouping and individual threads now explicitly check the two participants.
5. **Chat typing interrupted during refresh.** The composer stays editable during background
   loading; Send is still serialized. A successful send cannot erase a newer draft.
6. **Stripe completion left the buyer on the payment screen.** An acknowledged order triggers
   navigation to Orders, including recovery. The recorded order is retained before refresh.
   Navigation waits for a real back-stack entry, avoiding a graph-not-ready startup race.
7. **Toolbar Back skipped listing-wizard steps.** It now delegates to Android Back handling,
   which respects the form's previous-step behavior. The busy guard is registered after
   NavHost so System Back cannot bypass it during an active request.
8. **White reference cards appeared green; seller metrics were oversized.** Device captures
   identified Material surface tint and vertical stat stacking. Elevated surfaces keep a white tint and the stat
   icon/value share a compact row. Currency formatting is consistent across device locales.
9. **Old navigation wording.** Checkout recovery instructions now refer to Orders, not Purchases.

## What the checks do and do not prove

JVM regression tests cover acknowledgement ordering, failed refreshes, cancellation,
authentication failure propagation, conversation isolation, order eligibility and display rules.
Android instrumentation uses isolated fixture preferences and does not mutate the real marketplace.
It covers screen rendering, navigation, form reachability, typing while busy and confirmation guards.
The read-only Django checks validate response contracts for both roles. Actual counts and results
are in VERIFICATION.md.

This is not a claim that every possible bug has been eliminated. A failed/lost POST response is
still ambiguous without backend idempotency: refresh/reconcile before retrying a purchase or upload.
Real-account login/refresh, document-provider uploads, multi-device messaging, Stripe authorization,
moderation and a complete purchase/dispute lifecycle still need controlled end-to-end acceptance.

## Still needed for a complete production app

- Backend authorization, atomic inventory/transaction handling, idempotency and payment reconciliation.
- Production payment provider configuration, real seller payouts and compliance work.
- Backend support for saved addresses, courier tracking, dispute attachments, password-reset email,
  and social login if those design features are required.
- Release signing, production HTTPS hosting, Play Store readiness and accessibility/device coverage.

No shared marketplace products, orders, messages, reviews or disputes were created by this audit.
No changes were committed or pushed to GitHub.
