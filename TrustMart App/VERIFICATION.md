# Verification — 26 September 2026

> Historical report. For the latest 30 September website/API integration, consult
> [FINAL_PROJECT_INTEGRATION.md](../FINAL_PROJECT_INTEGRATION.md). Earlier device
> tests do not validate the newly integrated website features.

## Follow-up crash fix (latest build)

- Phone crash logs confirmed repeated infinite-height vertical-scroll exceptions in
  `com.trustmart.app`. The product-details missing-data branch nested `Page` inside
  another vertical scroller. It now renders a non-scrolling loading/error column.
- `gradlew.bat assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --offline --no-daemon`:
  **BUILD SUCCESSFUL**. All **24 JVM tests passed**; lint has **0 errors, 9 warnings**
  and 2 informational findings.
- Added **3 device regression tests** for missing detail, loading/failure/recovery,
  and stale detail from a different product. They compile, but **have not run**:
  the Samsung disconnected during this follow-up and remained unavailable.
- The new debug APK is built at `app/build/outputs/apk/debug/app-debug.apk`, but
  **this latest crash fix has not been installed on the phone**. Reconnect it to
  install the update and run the now-24-test instrumentation suite.
- The earlier 21 device checks below exercised populated product fixtures and
  missed the crashing branch; they are not evidence that this follow-up is device-verified.

## Earlier redesign checks (before the follow-up crash fix)

- `gradlew.bat assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --no-daemon`: **BUILD SUCCESSFUL**.
- **24 JVM unit tests**, 0 failures, 0 errors, 0 skipped: 10 contract tests,
  6 presentation checks and 8 action/retry/eligibility regression tests.
- **21 Android device tests passed** on the connected Samsung SM-A525F:
  15 reference screens plus guest/seller navigation, acknowledged-checkout navigation,
  busy Back handling, chat typing during refresh and explicit delivery confirmation.
- Lint: **0 errors, 9 warnings**. Remaining warnings concern deliberately pinned library versions compatible
  with the existing SDK 34 / Kotlin 1.9 toolchain; no lint baseline or error suppression was added.
- **22 authenticated, read-only Django endpoint checks**, including required native response fields,
  passed for buyer and seller roles. No product, purchase, dispute or message data was changed.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Separate Android application ID: `com.trustmart.app`.
- Installed the updated debug APK and test APK. The real MainActivity cold launch returned
  `Status: ok`; no AndroidRuntime errors were returned by the scoped process check.
- The configured PC API address matched the Wi-Fi address. The listener was confirmed on
  port 8000 and the read-only categories request returned HTTP 200. No server settings were changed.

## Not claimed as tested

- Comprehensive accessibility, small-screen/large-font coverage and long-running real-account use.
- Real file-provider uploads, Android Keystore behavior, activity/process recreation, and Wi-Fi/firewall access.
- End-to-end Stripe test authorization, payment recovery, simulated checkout, delivery confirmation,
  seller submission, moderation and multi-account chat interactions.
- Production payments, security certification, Play Store publishing or production hosting.

Use the physical-device acceptance checklist in README.md before demonstrating these flows.
The shared backend's production security/idempotency limitations are also documented there.

## Screenshot redesign verification scope

- Source reference: the 15 user-uploaded PNGs, with explicit approval to proceed without
  Figma design-context access. See UI_REDESIGN.md for screen mapping and API-related differences.
- A host-side Robolectric screen-rendering attempt was stopped after its runtime dependency
  download stalled. Those dependencies were removed; verification switched to the connected phone.
- The first **16 Android instrumentation tests passed** on the Samsung SM-A525F. The debug app
  and test APK were installed through the approved device-test command. Captured screen PNGs
  were inspected; this caught unintended surface tint and oversized seller metric cards.
- Android instrumentation layout/navigation tests are retained in app/src/androidTest.
  They use isolated session preferences and test-only fixtures, with screen loading disabled
  through Compose inspection mode; they do not create shared marketplace records.
- Captures are transferred by tools/pull-ui-screenshots.ps1 to app/build/reports/ui-screenshots.
  They contain isolated fixture data, not seeded products or real payment records.
- The expanded test pass initially exposed two navigation races; both were fixed and
  the complete rerun reported `OK (21 tests)`.

## Workspace note

A generated backend/ml_cache/recommender.pkl change appeared during the audit. It was
left untouched rather than reverted; no backend source changes or GitHub pushes were made.
