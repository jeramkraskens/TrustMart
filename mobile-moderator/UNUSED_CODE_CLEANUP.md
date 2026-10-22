# Moderator app unused-code cleanup

Reviewed application source, navigation, Android resources, and test references.
This list describes only this cleanup, separately from earlier bug fixes.

## Removed

| File | Removed code | Evidence |
| --- | --- | --- |
| `ui/verification/VerificationViewModel.kt` | `approve`, `reject`, private `act`, and `VerificationUiState.actioningId` | The queue only loads and opens reviews. Approve/reject buttons call `VerificationDetailViewModel`. |
| `ui/disputes/DisputeViewModel.kt` | `resolve` and `DisputeUiState.actioningId` | The queue only loads and opens cases. The decision dialog calls `DisputeDetailViewModel.resolve`. |
| `data/ApiService.kt` | Retrofit `refreshToken` endpoint declaration | Refresh requests are performed by `SessionInterceptor` using its separate OkHttp client. |
| `data/models/Models.kt` | `RefreshRequest` and `RefreshResponse` classes | Only the unused Retrofit endpoint referenced them. `RefreshedTokens` and response validation remain active. |
| `ui/home/HomeViewModel.kt` | Unread `tokenManager` instance and its import | Dashboard requests obtain the configured API through `RetrofitClient`. |
| `ui/login/LoginViewModel.kt` | Unread `LoginState.Success.role` value | The screen only checks for success. Role validation still runs before saving login tokens; success is now an object. |
| `ui/theme/Color.kt` | `Purple`, `PurpleLight`, `Brass`, `BrassLight` | No application or test references. The Brass entries were legacy aliases. |
| `app/build.gradle.kts` | Direct `ui-tooling-preview` dependency | No `Preview` annotation or preview API is used. Debug UI tooling remains available. |
| `res/values/themes.xml` | Unused `tools` XML namespace | No tools attributes occur in this resource. |

Removed these 18 response-only model properties because no screen or logic reads
them. Gson ignores additional server response keys; request bodies are unchanged.

- `User`: `email`, `isBanned`.
- `VerificationRequest`: `reviewedAt`, `listing`, `evidence`.
- `PaginatedResponse`: `count`, `previous` (pagination still follows `next`).
- `Dispute`: `transaction`, `raisedByName`, `buyerId`, `sellerId`, `createdAt`.
- `Message`: `listing`, `listingTitle`, `isRead`.
- `ListingDetail`: `description`, `whatsappEnabled`.
- `ListingPhoto`: `id`.

Removed seven additional unused imports:

- `MainActivity.kt`: `Paper`.
- `ui/ModerationUi.kt`: `BuildConfig`.
- `ui/chat/ChatViewModel.kt`: `BuildConfig`.
- `ui/notifications/NotificationViewModel.kt`: `BuildConfig`.
- `ui/disputes/DisputeScreen.kt`: `clickable`.
- `ui/disputes/DisputeDetailScreen.kt`: `Dispute`.
- `androidTest/ScreenSmokeTest.kt`: `MutableState`.

Imports for the removed queue actions were also removed with those methods.

## Kept after checking

All current screens are reachable through navigation. Manifest entry points,
launcher icons, backup/network resources, JSON field annotations, WebSocket
callbacks, session handling, and JUnit test methods have framework or application
callers and were retained.

## Verification

Validation command from the moderator project directory:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --offline --no-daemon --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process
```

Results: app APK and Android UI test APK built successfully; all 11 JVM tests
passed; lint reported zero errors and six existing warnings (launcher icons and
the debug HTTP configuration). The UI test APK was compiled but was not run on a
device during this cleanup. The moderator diff passed the whitespace check.
