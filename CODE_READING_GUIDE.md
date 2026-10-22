# How to read the TrustMart project

This project has one shared backend and three clients. Start with one feature,
such as login or listing creation, and follow it from the screen to the API.
The comments in the source explain file responsibilities and important decisions.

## 1. Understand the four main folders

| Folder | Responsibility | Start here |
| --- | --- | --- |
| `backend/` | Django API, database models, validation and access rules | `config/urls.py` |
| `frontend/` | React website for buyers, sellers, moderators and admins | `src/main.jsx`, then `src/App.jsx` |
| `TrustMart App/` | Native buyer/seller Android app | `app/src/main/java/com/trustmart/app/MainActivity.kt` |
| `mobile-moderator/` | Native Android app for moderators | `app/src/main/java/com/marketplace/moderator/MainActivity.kt` |

The Android apps and website call the same Django API. They do not each have
their own marketplace database. `Remove Items/` contains archived cleanup items,
not the active application source.

## 2. Follow a backend request

Read these files in order inside the relevant Django app:

1. `urls.py`: connects a URL to a view.
2. `views.py`: handles the request, chooses accessible records and performs actions.
3. `serializers.py`: validates incoming values and converts records into API data.
4. `models.py`: defines the stored fields and relationships.
5. `tests.py` and other `test_*.py` files: show expected behavior and failure cases.

`config/settings.py` connects the project to its database and configures installed
apps, authentication and uploaded files. `migrations/` records database schema
changes; do not treat migrations as disposable generated caches.

| Django app | Feature |
| --- | --- |
| `accounts` | Registration, login, profiles, roles and administrator analytics |
| `listings` | Products, categories, images, locations and wishlists |
| `verification` | Evidence review and listing approval/rejection |
| `transactions` | Orders and simulated escrow state |
| `disputes` | Complaints about orders and moderator resolutions |
| `reviews` | Purchase feedback and ratings |
| `messaging` | User conversations |
| `notifications` | In-app activity notices |
| `recommendations` | Search/view history and suggested products |
| `advertisements` | Promotional offers |

## 3. Follow a website action

`src/main.jsx` starts React. `src/App.jsx` selects a page based on the URL.
Files in `src/pages/` represent screens; `src/components/` contains shared controls.
The `admin/` and `moderator/` page folders hold those role-specific workspaces.

A typical flow is:

```text
User clicks a button on a page
  -> event handler prepares request data
  -> src/api/client.js sends the HTTP request
  -> Django validates permissions and input, then reads/writes records
  -> the page updates React state using the response
  -> React renders the updated screen
```

`useState` stores a component's changing values. `useEffect` runs work associated
with mounting or dependency changes. JSX is the HTML-like markup returned by a
React component. CSS files control its appearance.

For login, read `pages/Login.jsx`, `context/AuthContext.jsx`, `api/client.js`,
and `api/session.js`, then the backend `accounts/urls.py`, `views.py` and
`serializers.py`. An access token accompanies authenticated API requests; a
refresh token can obtain new access credentials.

`ProtectedRoute.jsx` controls which pages are shown. Backend permissions remain
responsible for enforcing access to the data itself.

## 4. Follow an Android action

For the buyer/seller app, read:

1. `MainActivity.kt`: startup, navigation and shared layout.
2. `ui/`: screen composables and reusable controls.
3. `AppViewModel.kt`: changing screen state and user actions.
4. `data/Api.kt`: HTTP requests, response handling, pagination and uploads.
5. `data/Models.kt`: Kotlin representations of API records.
6. `data/SessionStore.kt` and `SessionInterceptor.kt`: saved session and request authentication.

For the moderator app, read `MainActivity.kt`, then a matching screen and
ViewModel pair under `ui/`, such as `verification/VerificationDetailScreen.kt`
and `VerificationDetailViewModel.kt`. Continue into `data/ApiService.kt`,
`RetrofitClient.kt` and `ModerationRepository.kt` to follow the request.

A `@Composable` function describes UI. Observable state changes cause Compose
to redraw affected UI. A ViewModel holds screen data and actions. A `suspend`
function can pause while asynchronous work completes; its coroutine context
determines where the work executes.

## 5. Trace these important features

- **Create a listing:** website `pages/CreateListing.jsx` or Android
  `ui/SellerScreens.kt` -> backend `listings/views.py` and `serializers.py`.
  Text, photos and evidence are sent together using multipart form data.
- **Approve a listing:** moderator screen/ViewModel -> backend
  `verification/views.py`. The action updates review and listing status and
  creates a seller notification.
- **Buy a product:** client checkout UI -> backend `transactions/views.py`.
  The backend takes the buyer from the authenticated request and the price from
  the database. Checkout currently changes demo order state; it does not transfer money.
- **Recommend products:** backend `recommendations/views.py` -> `ml_engine.py`.
  Listing text is compared with a user's recent search/view history.

## 6. Useful terms in the code

| Term | Meaning here |
| --- | --- |
| API | HTTP interface used by the website and mobile apps |
| Queryset | Django description of which database records to retrieve |
| Serializer | API field conversion and input validation |
| Permission | Server rule deciding who may access an operation or record |
| Pagination | Returning a large result set in smaller pages |
| Repository | Client layer that fetches or saves data through the API |
| Atomic transaction | Groups database changes so they succeed or roll back together |
| `select_for_update()` | Locks selected database rows during a transaction on supporting databases |
| `listing__seller` | Django lookup following a listing relationship to its seller |

Read the existing tests after tracing a feature. They are practical examples of
the inputs and results the code expects. Dependency directories, build output and
caches are not useful starting points for learning the project's own code.
