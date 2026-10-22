# TrustMart unused-code cleanup

Reviewed main source, navigation, Android resources, dependencies, and references
from JVM and Android UI tests. This report covers this cleanup only.

## Removed

Eight unused response-model properties from `app/src/main/java/com/trustmart/app/data/Models.kt`:

| Model | Removed properties | Why they were unused |
| --- | --- | --- |
| `Advertisement` | `id` | Promotions render the advertisement content without reading its database ID. |
| `SearchSuggestion` | `id` | Selection uses `url`; display uses `label`, `detail`, and `kind`. |
| `SavedListing` | `id` | Wishlist state uses the listing ID and listing details. |
| `Message` | `listing` | Conversation rendering and grouping use participant IDs, message ID, content, and timestamp. |
| `Review` | `reviewee` | Queries already select the seller's reviews. Eligibility uses transaction and reviewer IDs. |
| `Dispute` | `listingTitle`, `resolution` | Tracking uses the order's listing title and displays dispute status, reason, and moderator resolution notes. |
| `Verification` | `notes` | Seller screens use listing/status and moderator feedback. |

Gson ignores extra response keys, so these removals do not require backend changes.
Outgoing request fields are retained, including a new message's listing ID,
reviewee ID when submitting a review, and seller evidence notes.

Removed the unused `ArrowForward` icon import from
`app/src/main/java/com/trustmart/app/ui/MarketplaceScreens.kt`.

Updated constructor arguments in existing `ActionFlowTest`, `ContractTest`,
`PresentationTest`, and `ScreenLayoutTest` fixtures to match the smaller response
models. No tests or assertions were removed.

## Kept after checking

All current screens and top-level application helpers have callers. Category
icons are used by the marketplace screens. Existing checkout recovery handles
persisted records, and is reachable from Orders. Chat, notifications, seller
listing actions, moderation feedback, API operations, and session protections
remain in use. Resources have manifest/style references or Android version
qualifiers. No unused resource or dependency was identified for removal.

Framework entry points and overrides, such as `MainActivity`, WebSocket callbacks,
OkHttp interception, and JUnit methods, are called by their frameworks.

## Verification

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --offline --no-daemon --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process
```

Results: app APK and Android UI test APK built successfully; all **43 JVM tests
passed**; lint reported **zero issues**. The TrustMart diff passed the whitespace
check. Device UI tests were compiled but were not rerun on a device for this
cleanup.
